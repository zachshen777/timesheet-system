package com.zach.mytools.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 定时任务展示 DTO
 * <p>
 * 在任务配置之外，附带调度器运行时的实时信息（是否已注册、下次执行时间、是否正在执行）。
 */
@Data
public class JobInfoDTO {

    private Long id;
    private String jobName;
    private String description;
    private String execType;
    private String cronExpression;
    private Long fixedInterval;
    private String className;
    private Integer status;
    private String remark;
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /** 下次执行时间（未调度/已禁用时为 null） */
    private LocalDateTime nextRunTime;

    /** 是否已注册到调度器 */
    private boolean scheduled;

    /** 当前是否正在执行 */
    private boolean running;
}
