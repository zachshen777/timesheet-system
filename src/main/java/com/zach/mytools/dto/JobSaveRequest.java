package com.zach.mytools.dto;

import lombok.Data;

/**
 * 定时任务新增/编辑请求
 */
@Data
public class JobSaveRequest {

    /** 任务名称 */
    private String jobName;

    /** 任务描述 */
    private String description;

    /** 执行类型：CRON / FIXED_RATE */
    private String execType;

    /** cron 表达式（6 位：秒 分 时 日 月 周） */
    private String cronExpression;

    /** 固定间隔时长（秒） */
    private Long fixedInterval;

    /** 执行的 Java 类全限定名 */
    private String className;

    /** 状态：1启用 0禁用（为空时新增默认禁用） */
    private Integer status;

    /** 备注 */
    private String remark;
}
