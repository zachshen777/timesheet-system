package com.zach.mytools.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 定时任务配置实体
 * <p>
 * 管理员在「系统设置 → 定时任务」中维护，修改后由 JobSchedulerManager 实时重新注册，无需重启服务。
 */
@Data
@TableName("sys_job")
public class SysJob {

    /** 执行类型：cron 表达式 */
    public static final String TYPE_CRON = "CRON";
    /** 执行类型：固定间隔（每 N 秒一次） */
    public static final String TYPE_FIXED_RATE = "FIXED_RATE";

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 任务名称 */
    @TableField("job_name")
    private String jobName;

    /** 任务描述 */
    @TableField("description")
    private String description;

    /** 执行类型：CRON / FIXED_RATE */
    @TableField("exec_type")
    private String execType;

    /** cron 表达式（6 位：秒 分 时 日 月 周），execType=CRON 时必填 */
    // updateStrategy=ALWAYS：在 CRON ↔ 固定间隔 之间切换时，必须能把不用的那一列真正置为 NULL
    // （MyBatis-Plus 默认忽略 null 字段，不加这个会残留旧配置）
    @TableField(value = "cron_expression", updateStrategy = FieldStrategy.ALWAYS)
    private String cronExpression;

    /** 固定间隔时长（秒），execType=FIXED_RATE 时必填 */
    @TableField(value = "fixed_interval", updateStrategy = FieldStrategy.ALWAYS)
    private Long fixedInterval;

    /** 执行的 Java 类全限定名（如 com.zach.mytools.job.task.HeartbeatLogJob） */
    @TableField("class_name")
    private String className;

    /** 状态：1启用 0禁用 */
    @TableField("status")
    private Integer status;

    /** 备注 */
    @TableField("remark")
    private String remark;

    /** 创建人（管理员用户名） */
    @TableField("created_by")
    private String createdBy;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
