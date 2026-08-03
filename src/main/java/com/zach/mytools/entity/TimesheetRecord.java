package com.zach.mytools.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 工时填报记录实体
 */
@Data
@TableName("timesheet_record")
public class TimesheetRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 员工ID（关联 employee 表主键） */
    @TableField("emp_id")
    private Long empId;

    /** 填报日期 */
    @TableField("date")
    private LocalDate date;

    /** 项目名称 */
    @TableField("project")
    private String project;

    /** 任务名称 */
    @TableField("task")
    private String task;

    /** 备注 */
    @TableField("remark")
    private String remark;

    /** 工时（小时） */
    @TableField("work_hours")
    private BigDecimal workHours;

    /** 加班时长（小时） */
    @TableField("overtime_hours")
    private BigDecimal overtimeHours;

    /** 状态：DRAFT-草稿 SUBMITTED-已提交 */
    @TableField("status")
    private String status;

    /** 记录创建时间（插入时自动填充） */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /** 记录更新时间（插入和更新时自动填充） */
    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
