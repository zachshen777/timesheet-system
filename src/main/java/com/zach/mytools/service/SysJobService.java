package com.zach.mytools.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zach.mytools.dto.JobClassOptionDTO;
import com.zach.mytools.dto.JobInfoDTO;
import com.zach.mytools.dto.JobSaveRequest;
import com.zach.mytools.dto.PageResult;
import com.zach.mytools.entity.SysJob;
import com.zach.mytools.entity.SysJobLog;
import com.zach.mytools.job.JobSchedulerManager;
import com.zach.mytools.job.ScheduledJob;
import com.zach.mytools.mapper.SysJobLogMapper;
import com.zach.mytools.mapper.SysJobMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 定时任务业务服务
 * <p>
 * 所有写操作成功后都会调用 {@link JobSchedulerManager#schedule(SysJob)} 重新注册，
 * 因此「新增/编辑/启停」在保存后立即生效，无需重启服务。
 */
@Slf4j
@Service
public class SysJobService {

    private static final int NAME_MAX = 100;
    private static final int DESC_MAX = 500;
    private static final int CLASS_MAX = 255;
    private static final int CRON_MAX = 100;

    private final SysJobMapper jobMapper;
    private final SysJobLogMapper jobLogMapper;
    private final JobSchedulerManager schedulerManager;
    private final ApplicationContext applicationContext;
    private final long minIntervalSeconds;

    public SysJobService(SysJobMapper jobMapper,
                         SysJobLogMapper jobLogMapper,
                         JobSchedulerManager schedulerManager,
                         ApplicationContext applicationContext,
                         @Value("${mytools.job.min-interval-seconds:5}") long minIntervalSeconds) {
        this.jobMapper = jobMapper;
        this.jobLogMapper = jobLogMapper;
        this.schedulerManager = schedulerManager;
        this.applicationContext = applicationContext;
        this.minIntervalSeconds = minIntervalSeconds;
    }

    // ================================================================
    // 任务查询
    // ================================================================

    /**
     * 任务列表（附带调度器运行时信息：是否已注册、下次执行时间、是否正在执行）
     */
    public List<JobInfoDTO> listAll() {
        List<SysJob> jobs = jobMapper.selectList(new LambdaQueryWrapper<SysJob>().orderByAsc(SysJob::getId));
        return jobs.stream().map(this::toDTO).collect(Collectors.toList());
    }

    // ================================================================
    // 任务增删改（改动即时生效）
    // ================================================================

    /**
     * 新增任务
     */
    public SysJob create(JobSaveRequest request, String operator) {
        validate(request);

        SysJob job = new SysJob();
        applyRequest(job, request);
        job.setCreatedBy(operator);
        if (job.getStatus() == null) {
            job.setStatus(0);
        }
        jobMapper.insert(job);

        // 关键：注册到调度器 → 立即生效（新增默认禁用，启用时会在 toggleStatus 中注册）
        if (isEnabled(job)) {
            boolean ok = schedulerManager.schedule(job);
            log.info("新增定时任务: {} ({}) 已启用，注册结果={}", job.getJobName(), job.getClassName(), ok);
        } else {
            log.info("新增定时任务: {} ({}) 默认禁用", job.getJobName(), job.getClassName());
        }
        return job;
    }

    /**
     * 编辑任务：先落库，再用最新配置重新注册（实时生效）
     */
    public SysJob update(Long id, JobSaveRequest request) {
        SysJob job = requireJob(id);
        validate(request);

        applyRequest(job, request);
        jobMapper.updateById(job);

        schedulerManager.schedule(job); // 内部会先 cancel 再按新配置注册；已禁用则只取消
        log.info("编辑定时任务: id={} 名称={} 状态={}", id, job.getJobName(), isEnabled(job) ? "启用" : "禁用");
        return job;
    }

    /**
     * 启用 / 禁用切换
     */
    public SysJob toggleStatus(Long id) {
        SysJob job = requireJob(id);
        job.setStatus(isEnabled(job) ? 0 : 1);
        jobMapper.updateById(job);

        if (isEnabled(job)) {
            schedulerManager.schedule(job);
        } else {
            schedulerManager.cancel(job.getId());
        }
        log.info("定时任务{}: {} ({})", isEnabled(job) ? "已启用" : "已禁用", job.getJobName(), job.getClassName());
        return job;
    }

    /**
     * 立即执行一次（不影响原有调度计划）
     */
    public void triggerOnce(Long id) {
        SysJob job = requireJob(id);
        schedulerManager.triggerOnce(job);
        log.info("手动触发定时任务: {} ({})", job.getJobName(), job.getClassName());
    }

    /**
     * 删除任务（先取消调度；执行日志保留，便于事后追溯）
     */
    public void delete(Long id) {
        SysJob job = requireJob(id);
        schedulerManager.cancel(id);
        jobMapper.deleteById(id);
        log.info("删除定时任务: {} ({})", job.getJobName(), job.getClassName());
    }

    // ================================================================
    // 执行日志
    // ================================================================

    /**
     * 执行日志分页查询
     *
     * @param startDate 起始日期（yyyy-MM-dd 或 yyyy-MM-dd HH:mm:ss）
     * @param endDate   结束日期（同上）
     */
    public PageResult<SysJobLog> pageLogs(int page, int size, Long jobId, String status,
                                          String startDate, String endDate) {
        LambdaQueryWrapper<SysJobLog> qw = new LambdaQueryWrapper<>();
        if (jobId != null) {
            qw.eq(SysJobLog::getJobId, jobId);
        }
        if (status != null && !status.isBlank()) {
            qw.eq(SysJobLog::getStatus, status.trim().toUpperCase());
        }
        LocalDateTime start = parseTime(startDate, false);
        LocalDateTime end = parseTime(endDate, true);
        if (start != null) {
            qw.ge(SysJobLog::getStartTime, start);
        }
        if (end != null) {
            qw.le(SysJobLog::getStartTime, end);
        }
        qw.orderByDesc(SysJobLog::getId);

        IPage<SysJobLog> result = jobLogMapper.selectPage(new Page<>(page, size), qw);
        return new PageResult<>(result.getRecords(), result.getTotal());
    }

    /**
     * 清理执行日志；jobId 为空表示清空全部
     *
     * @return 删除条数
     */
    public int clearLogs(Long jobId) {
        if (jobId != null) {
            requireJob(jobId);
            int deleted = jobLogMapper.deleteByJobId(jobId);
            log.info("清理任务执行日志: jobId={} 共 {} 条", jobId, deleted);
            return deleted;
        }
        int deleted = jobLogMapper.delete(new LambdaQueryWrapper<>());
        log.info("清空全部任务执行日志: 共 {} 条", deleted);
        return deleted;
    }

    // ================================================================
    // 候选任务类（前端下拉用）
    // ================================================================

    /**
     * 返回容器中所有 ScheduledJob 实现，方便管理员直接选择而不是手敲全类名
     * （前端下拉支持输入过滤 + 手动输入任意全类名）
     */
    public List<JobClassOptionDTO> listCandidateClasses() {
        List<JobClassOptionDTO> options = new ArrayList<>();
        try {
            Map<String, ScheduledJob> beans = applicationContext.getBeansOfType(ScheduledJob.class);
            beans.values().forEach(bean -> {
                // 兼容 CGLIB 代理：拿目标类，避免出现 xxx$$SpringCGLIB$$0 这种名字
                Class<?> target = AopUtils.getTargetClass(bean);
                options.add(new JobClassOptionDTO(target.getName(), target.getSimpleName(), "SPRING_BEAN"));
            });
        } catch (Exception e) {
            log.error("扫描候选任务类失败: {}", e.getMessage());
        }
        options.sort((a, b) -> a.getClassName().compareTo(b.getClassName()));
        return options;
    }

    // ================================================================
    // 内部方法
    // ================================================================

    private SysJob requireJob(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("任务ID不能为空");
        }
        SysJob job = jobMapper.selectById(id);
        if (job == null) {
            throw new IllegalArgumentException("任务不存在（可能已被删除）");
        }
        return job;
    }

    private boolean isEnabled(SysJob job) {
        return job.getStatus() != null && job.getStatus() == 1;
    }

    private void applyRequest(SysJob job, JobSaveRequest request) {
        job.setJobName(request.getJobName().trim());
        job.setDescription(trimToNull(request.getDescription()));
        job.setExecType(request.getExecType().trim().toUpperCase());
        job.setClassName(request.getClassName().trim());
        job.setRemark(trimToNull(request.getRemark()));

        if (SysJob.TYPE_CRON.equals(job.getExecType())) {
            job.setCronExpression(request.getCronExpression().trim());
            job.setFixedInterval(null);
        } else {
            job.setFixedInterval(request.getFixedInterval());
            job.setCronExpression(null);
        }
        if (request.getStatus() != null) {
            job.setStatus(request.getStatus() == 1 ? 1 : 0);
        }
    }

    /**
     * 参数校验：名称/类型/cron 表达式/固定间隔/类名可执行性
     */
    private void validate(JobSaveRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("请求参数不能为空");
        }
        String name = trimToNull(request.getJobName());
        if (name == null) {
            throw new IllegalArgumentException("任务名称不能为空");
        }
        if (name.length() > NAME_MAX) {
            throw new IllegalArgumentException("任务名称不能超过 " + NAME_MAX + " 个字符");
        }
        if (request.getDescription() != null && request.getDescription().length() > DESC_MAX) {
            throw new IllegalArgumentException("任务描述不能超过 " + DESC_MAX + " 个字符");
        }

        String execType = request.getExecType() == null ? "" : request.getExecType().trim().toUpperCase();
        if (SysJob.TYPE_CRON.equals(execType)) {
            String cron = trimToNull(request.getCronExpression());
            if (cron == null) {
                throw new IllegalArgumentException("执行类型为 CRON 时，必须填写 cron 表达式");
            }
            if (cron.length() > CRON_MAX) {
                throw new IllegalArgumentException("cron 表达式过长");
            }
            try {
                CronExpression.parse(cron);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("cron 表达式不合法（需 6 位：秒 分 时 日 月 周）：" + e.getMessage());
            }
        } else if (SysJob.TYPE_FIXED_RATE.equals(execType)) {
            Long interval = request.getFixedInterval();
            if (interval == null) {
                throw new IllegalArgumentException("执行类型为固定间隔时，必须填写间隔秒数");
            }
            if (interval < minIntervalSeconds) {
                throw new IllegalArgumentException("固定间隔不能小于 " + minIntervalSeconds + " 秒（避免过于频繁地占用资源）");
            }
            if (interval > 30L * 24 * 3600) {
                throw new IllegalArgumentException("固定间隔不能超过 30 天");
            }
        } else {
            throw new IllegalArgumentException("执行类型只能是 CRON（cron表达式）或 FIXED_RATE（固定间隔）");
        }

        String className = trimToNull(request.getClassName());
        if (className == null) {
            throw new IllegalArgumentException("执行类不能为空，请填写 Java 全限定类名");
        }
        if (className.length() > CLASS_MAX) {
            throw new IllegalArgumentException("类名过长");
        }
        checkExecutableClass(className);
    }

    /**
     * 校验类名可加载且可执行，避免管理员填错后要等到执行时才发现
     */
    private void checkExecutableClass(String className) {
        Class<?> clazz;
        try {
            // initialize=false：只加载类结构，不触发静态初始化
            clazz = Class.forName(className, false, getClass().getClassLoader());
        } catch (Throwable e) {
            throw new IllegalArgumentException("找不到类「" + className
                    + "」，请填写完整全限定名（如 com.zach.mytools.job.task.HeartbeatLogJob）");
        }
        boolean executable = ScheduledJob.class.isAssignableFrom(clazz)
                || Runnable.class.isAssignableFrom(clazz);
        if (!executable) {
            try {
                clazz.getMethod("execute");
                executable = true;
            } catch (NoSuchMethodException ignored) {
                // 既不是 ScheduledJob/Runnable，也没有 execute() → 判定不可执行
            }
        }
        if (!executable) {
            throw new IllegalArgumentException("类「" + className
                    + "」未实现 ScheduledJob/Runnable，也没有无参的 execute() 方法，无法作为定时任务执行");
        }
    }

    private LocalDateTime parseTime(String value, boolean endOfDay) {
        String v = trimToNull(value);
        if (v == null) {
            return null;
        }
        v = v.replace(' ', 'T');
        if (v.length() == 10) {
            v = v + (endOfDay ? "T23:59:59" : "T00:00:00");
        }
        try {
            return LocalDateTime.parse(v);
        } catch (Exception e) {
            throw new IllegalArgumentException("时间格式不正确：" + value + "（应为 yyyy-MM-dd 或 yyyy-MM-dd HH:mm:ss）");
        }
    }

    private String trimToNull(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private JobInfoDTO toDTO(SysJob job) {
        JobInfoDTO dto = new JobInfoDTO();
        dto.setId(job.getId());
        dto.setJobName(job.getJobName());
        dto.setDescription(job.getDescription());
        dto.setExecType(job.getExecType());
        dto.setCronExpression(job.getCronExpression());
        dto.setFixedInterval(job.getFixedInterval());
        dto.setClassName(job.getClassName());
        dto.setStatus(job.getStatus());
        dto.setRemark(job.getRemark());
        dto.setCreatedBy(job.getCreatedBy());
        dto.setCreatedAt(job.getCreatedAt());
        dto.setUpdatedAt(job.getUpdatedAt());
        dto.setScheduled(schedulerManager.isScheduled(job.getId()));
        dto.setRunning(schedulerManager.isRunning(job.getId()));
        dto.setNextRunTime(schedulerManager.getNextRunTime(job.getId()));
        return dto;
    }
}
