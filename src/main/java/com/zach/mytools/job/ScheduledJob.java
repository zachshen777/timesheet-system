package com.zach.mytools.job;

/**
 * 定时任务接口
 * <p>
 * 约定：任务类实现本接口并实现 {@link #execute()}，然后把「全限定类名」配置到 sys_job.class_name 即可。
 * <p>
 * 兼容性说明：调度器执行任务时的查找顺序为
 * <ol>
 *     <li>实现 ScheduledJob → 调用 execute()</li>
 *     <li>实现 Runnable → 调用 run()</li>
 *     <li>以上都不是 → 反射查找无参的 execute() / run() 方法并调用</li>
 * </ol>
 * 也就是说，随便写一个「带无参 execute() 方法的普通类」也能被调度，不必依赖本接口。
 * <p>
 * 任务类建议声明为 Spring Bean（@Component），这样可以直接注入 Mapper / Service；
 * 若未注册为 Bean，调度器会用反射 new 出实例（此时依赖注入不生效）。
 */
public interface ScheduledJob {

    /**
     * 任务执行入口。抛出的任何异常都会被调度器捕获并写入 sys_job_log.error_msg。
     */
    void execute() throws Exception;
}
