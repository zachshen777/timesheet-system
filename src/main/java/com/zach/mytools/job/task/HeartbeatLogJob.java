package com.zach.mytools.job.task;

import com.zach.mytools.job.ScheduledJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 示例任务：系统心跳
 * <p>
 * 每隔一段时间打印一行日志与 JVM 内存占用，用来验证「调度链路是否正常」。
 * 在「系统设置 → 定时任务」里把执行类填 com.zach.mytools.job.task.HeartbeatLogJob 即可。
 */
@Slf4j
@Component
public class HeartbeatLogJob implements ScheduledJob {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public void execute() {
        Runtime runtime = Runtime.getRuntime();
        long usedMb = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024;
        long maxMb = runtime.maxMemory() / 1024 / 1024;
        log.info("[心跳] {} 应用运行正常，堆内存 {}/{} MB", LocalDateTime.now().format(FMT), usedMb, maxMb);
    }
}
