package com.zach.mytools.job;

import com.zach.mytools.entity.SysJob;
import com.zach.mytools.entity.SysJobLog;
import com.zach.mytools.mapper.SysJobLogMapper;
import com.zach.mytools.mapper.SysJobMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * 定时任务调度器
 * <p>
 * 核心能力：
 * <ul>
 *     <li><b>动态调度</b>：每个任务持有一个 {@link ScheduledFuture}，改配置/启停时先 cancel 再重新注册，
 *         所以「修改后实时生效，无需重启服务」。</li>
 *     <li><b>两种模式</b>：CRON（{@link CronTrigger}，6 位表达式）/ 固定间隔（按秒 scheduleAtFixedRate）。</li>
 *     <li><b>反射执行</b>：按 class_name 找 Spring Bean，找不到就反射 new 实例，再调用其执行方法。</li>
 *     <li><b>执行日志</b>：每次执行都写 sys_job_log（开始/结束时间、耗时、状态、异常堆栈）。</li>
 *     <li><b>防重入</b>：同一任务上一次还没跑完时，本轮直接跳过并记 SKIPPED，避免任务堆积。</li>
 * </ul>
 */
@Slf4j
@Component
public class JobSchedulerManager implements ApplicationRunner {

    private static final String TRIGGER_CRON = "CRON";
    private static final String TRIGGER_FIXED_RATE = "FIXED_RATE";
    private static final String TRIGGER_MANUAL = "MANUAL";

    /** 异常信息入库时的最大长度（避免超长堆栈撑爆 TEXT 字段） */
    private static final int ERROR_MSG_MAX = 2000;

    private final SysJobMapper jobMapper;
    private final SysJobLogMapper jobLogMapper;
    private final ApplicationContext applicationContext;
    private final int poolSize;

    /** Spring 提供的可动态增删任务的线程池调度器 */
    private final ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();

    /** jobId → 已注册的调度句柄（用它来计算下次执行时间、取消调度） */
    private final Map<Long, ScheduledFuture<?>> futures = new ConcurrentHashMap<>();

    /** 正在执行中的 jobId 集合（防重入） */
    private final Set<Long> runningJobs = ConcurrentHashMap.newKeySet();

    public JobSchedulerManager(SysJobMapper jobMapper,
                               SysJobLogMapper jobLogMapper,
                               ApplicationContext applicationContext,
                               @Value("${mytools.job.pool-size:5}") int poolSize) {
        this.jobMapper = jobMapper;
        this.jobLogMapper = jobLogMapper;
        this.applicationContext = applicationContext;
        this.poolSize = poolSize;
    }

    // ================================================================
    // 生命周期
    // ================================================================

    @PostConstruct
    public void initScheduler() {
        scheduler.setPoolSize(poolSize);
        scheduler.setThreadNamePrefix("mytools-job-");
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(30);
        scheduler.setRemoveOnCancelPolicy(true);
        scheduler.initialize();
        log.info("定时任务调度器初始化完成：线程池大小={}", poolSize);
    }

    /**
     * 应用完全启动后（此时 Mapper 已就绪）再把库里启用的任务注册进来。
     * 放在这里而不是 @PostConstruct，是为了避免在容器尚未就绪时访问数据库。
     */
    @Override
    public void run(ApplicationArguments args) {
        try {
            reloadAll();
        } catch (Exception e) {
            // 不能让单个模块的异常影响整个服务启动
            log.error("定时任务初始化失败（服务仍会正常启动）：{}", e.getMessage(), e);
        }
    }

    @PreDestroy
    public void destroy() {
        log.info("定时任务调度器关闭，注销全部任务…");
        cancelAll();
        scheduler.shutdown();
    }

    // ================================================================
    // 注册 / 注销
    // ================================================================

    /**
     * 全量重载：先全部注销，再把库中启用的任务注册一遍
     */
    public synchronized void reloadAll() {
        cancelAll();
        List<SysJob> jobs;
        try {
            jobs = jobMapper.selectList(null);
        } catch (Exception e) {
            log.error("读取定时任务配置失败：{}", e.getMessage());
            return;
        }
        int registered = 0;
        for (SysJob job : jobs) {
            if (isEnabled(job)) {
                if (schedule(job)) {
                    registered++;
                }
            }
        }
        log.info("定时任务加载完成：共 {} 条配置，其中 {} 个已启用并注册", jobs.size(), registered);
    }

    /**
     * 注册（或重新注册）单个任务 —— 先 cancel 旧的，再按最新配置注册，
     * 这是「修改配置后实时生效」的关键。
     *
     * @return 是否成功注册（已禁用、配置非法时为 false）
     */
    public synchronized boolean schedule(SysJob job) {
        if (job == null || job.getId() == null) {
            return false;
        }
        cancel(job.getId());

        if (!isEnabled(job)) {
            return false;
        }

        try {
            boolean cronMode = SysJob.TYPE_CRON.equals(job.getExecType());
            Runnable task = () -> execute(job, cronMode ? TRIGGER_CRON : TRIGGER_FIXED_RATE);

            ScheduledFuture<?> future;
            if (cronMode) {
                // CronTrigger 支持 6 位表达式（秒 分 时 日 月 周）
                future = scheduler.schedule(task, new CronTrigger(job.getCronExpression()));
            } else {
                long seconds = job.getFixedInterval() == null ? 60L : job.getFixedInterval();
                future = scheduler.scheduleAtFixedRate(task, Duration.ofSeconds(seconds));
            }

            // 注意：触发器算不出「下一次执行时间」时 schedule() 会返回 null，
            // 直接 put 进 ConcurrentHashMap 会 NPE，这里显式拦一下
            if (future == null) {
                log.warn("任务注册失败: id={} 名称={} 原因=触发器没有下一次执行时间（规则={}）",
                        job.getId(), job.getJobName(),
                        cronMode ? job.getCronExpression() : ("每 " + job.getFixedInterval() + " 秒"));
                return false;
            }

            futures.put(job.getId(), future);
            log.info("任务已注册: id={} 名称={} 类型={} 规则={}", job.getId(), job.getJobName(), job.getExecType(),
                    cronMode ? job.getCronExpression() : ("每 " + job.getFixedInterval() + " 秒"));
            return true;
        } catch (Exception e) {
            log.error("任务注册失败: id={} 名称={} 原因={}", job.getId(), job.getJobName(), e.getMessage());
            return false;
        }
    }

    /**
     * 注销单个任务的调度（不打断正在执行的那一次）
     */
    public synchronized void cancel(Long jobId) {
        if (jobId == null) {
            return;
        }
        ScheduledFuture<?> future = futures.remove(jobId);
        if (future != null) {
            future.cancel(false);
            log.info("任务已取消调度: id={}", jobId);
        }
    }

    private synchronized void cancelAll() {
        for (Long jobId : new ArrayList<>(futures.keySet())) {
            cancel(jobId);
        }
    }

    // ================================================================
    // 运行时状态（给前端展示用）
    // ================================================================

    public boolean isScheduled(Long jobId) {
        if (jobId == null) {
            return false;
        }
        ScheduledFuture<?> future = futures.get(jobId);
        return future != null && !future.isCancelled() && !future.isDone();
    }

    public boolean isRunning(Long jobId) {
        return jobId != null && runningJobs.contains(jobId);
    }

    /**
     * 下次执行时间；未注册或已结束时返回 null
     */
    public LocalDateTime getNextRunTime(Long jobId) {
        if (jobId == null) {
            return null;
        }
        ScheduledFuture<?> future = futures.get(jobId);
        if (future == null || future.isCancelled() || future.isDone()) {
            return null;
        }
        try {
            long delayMs = future.getDelay(TimeUnit.MILLISECONDS);
            if (delayMs < 0) {
                delayMs = 0;
            }
            return LocalDateTime.ofInstant(Instant.now().plusMillis(delayMs), ZoneId.systemDefault());
        } catch (Exception e) {
            return null;
        }
    }

    // ================================================================
    // 手动触发
    // ================================================================

    /**
     * 立即执行一次（不改变原有调度计划），异步执行，结果写入执行日志
     */
    public void triggerOnce(SysJob job) {
        // schedule(task, Instant) 是 TaskScheduler 的标准用法：交给同一个调度线程池，立即异步执行
        scheduler.schedule(() -> execute(job, TRIGGER_MANUAL), Instant.now());
    }

    // ================================================================
    // 执行核心
    // ================================================================

    private void execute(SysJob job, String triggerType) {
        Long jobId = job.getId();

        // 防重入：上一次还没跑完就跳过本轮，避免慢任务越堆越多
        if (!runningJobs.add(jobId)) {
            String msg = "上一次执行尚未结束，本次跳过";
            log.warn("任务[{}] {}", job.getJobName(), msg);
            saveLog(job, triggerType, LocalDateTime.now(), LocalDateTime.now(), SysJobLog.STATUS_SKIPPED, msg);
            return;
        }

        LocalDateTime start = LocalDateTime.now();
        long startMs = System.currentTimeMillis();
        String status = SysJobLog.STATUS_SUCCESS;
        String errorMsg = null;
        try {
            Object instance = resolveInstance(job.getClassName());
            invokeJob(instance);
            log.info("任务执行成功: [{}] {} 耗时 {}ms", job.getJobName(), job.getClassName(),
                    System.currentTimeMillis() - startMs);
        } catch (Throwable t) {
            status = SysJobLog.STATUS_FAILED;
            errorMsg = buildError(t);
            log.error("任务执行失败: [{}] {} - {}", job.getJobName(), job.getClassName(), t.toString());
        } finally {
            runningJobs.remove(jobId);
        }

        saveLog(job, triggerType, start, LocalDateTime.now(), status, errorMsg);
    }

    /**
     * 按全限定类名取得可执行的实例：
     * 优先复用 Spring 容器里的 Bean（这样任务类可以正常注入 Mapper/Service），
     * 容器里没有时再按需求用反射实例化。
     */
    public Object resolveInstance(String className) throws Exception {
        Class<?> clazz = Class.forName(className);
        try {
            return applicationContext.getBean(clazz);
        } catch (BeansException notABean) {
            // 容器里没有该 Bean → 反射创建（要求存在无参构造）
            return clazz.getDeclaredConstructor().newInstance();
        }
    }

    /**
     * 调用任务方法：ScheduledJob → Runnable → 反射无参 execute()/run()
     */
    private void invokeJob(Object instance) throws Exception {
        if (instance instanceof ScheduledJob scheduledJob) {
            scheduledJob.execute();
            return;
        }
        if (instance instanceof Runnable runnable) {
            runnable.run();
            return;
        }
        Method method = findNoArgMethod(instance.getClass(), "execute", "run");
        if (method == null) {
            throw new IllegalStateException("类 " + instance.getClass().getName()
                    + " 未实现 ScheduledJob / Runnable，也没有无参的 execute() / run() 方法，无法执行");
        }
        method.invoke(instance);
    }

    private Method findNoArgMethod(Class<?> clazz, String... names) {
        for (String name : names) {
            try {
                return clazz.getMethod(name);
            } catch (NoSuchMethodException ignored) {
                // 继续尝试下一个名字
            }
        }
        return null;
    }

    /**
     * 拼装可读的异常信息（若是反射调用的包装异常则取根因，便于定位业务问题）
     */
    private String buildError(Throwable t) {
        Throwable cause = t;
        while (cause instanceof InvocationTargetException && cause.getCause() != null) {
            cause = cause.getCause();
        }
        StringWriter sw = new StringWriter();
        cause.printStackTrace(new PrintWriter(sw));
        String stack = sw.toString();
        return stack.length() > ERROR_MSG_MAX ? stack.substring(0, ERROR_MSG_MAX) + "\n...(已截断)" : stack;
    }

    private void saveLog(SysJob job, String triggerType, LocalDateTime start, LocalDateTime end,
                         String status, String errorMsg) {
        try {
            SysJobLog row = new SysJobLog();
            row.setJobId(job.getId());
            row.setJobName(job.getJobName());
            row.setClassName(job.getClassName());
            row.setTriggerType(triggerType);
            row.setStartTime(start);
            row.setEndTime(end);
            row.setCostMs(Duration.between(start, end).toMillis());
            row.setStatus(status);
            row.setErrorMsg(errorMsg);
            jobLogMapper.insert(row);
        } catch (Exception e) {
            // 日志写失败不能影响任务本身
            log.error("写入任务执行日志失败: jobId={} 原因={}", job.getId(), e.getMessage());
        }
    }

    private boolean isEnabled(SysJob job) {
        return job.getStatus() != null && job.getStatus() == 1;
    }
}
