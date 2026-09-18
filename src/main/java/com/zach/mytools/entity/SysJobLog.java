package com.zach.mytools.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 定时任务执行日志实体
 * <p>
 * 每次任务执行（含定时触发与手动触发）都会落一条记录，便于排查「任务到底跑没跑、为什么失败」。
 */
@Data
@TableName("sys_job_log")
public class SysJobLog {

    /** 执行状态：成功 */
    public static final String STATUS_SUCCESS = "SUCCESS";
    /** 执行状态：失败 */
    public static final String STATUS_FAILED = "FAILED";
    /** 执行状态：跳过（上一次执行尚未结束） */
    public static final String STATUS_SKIPPED = "SKIPPED";

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 任务ID（关联 sys_job.id） */
    @TableField("job_id")
    private Long jobId;

    /** 任务名称（冗余保存，任务删除后日志仍可读） */
    @TableField("job_name")
    private String jobName;

    /** 执行类全限定名（冗余保存） */
    @TableField("class_name")
    private String className;

    /** 触发方式：CRON / FIXED_RATE / MANUAL */
    @TableField("trigger_type")
    private String triggerType;

    /** 任务开始时间 */
    @TableField("start_time")
    private LocalDateTime startTime;

    /** 任务结束时间 */
    @TableField("end_time")
    private LocalDateTime endTime;

    /** 耗时（毫秒） */
    @TableField("cost_ms")
    private Long costMs;

    /** 执行状态：SUCCESS / FAILED / SKIPPED */
    @TableField("status")
    private String status;

    /** 异常信息（失败时的异常堆栈，截断保存） */
    @TableField("error_msg")
    private String errorMsg;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
