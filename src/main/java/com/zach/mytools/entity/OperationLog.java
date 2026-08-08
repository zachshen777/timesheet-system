package com.zach.mytools.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 操作日志实体（广播日志）
 */
@Data
@TableName("operation_log")
public class OperationLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 操作员工ID（关联 employee.id） */
    @TableField("emp_id")
    private Long empId;

    /** 操作员工姓名 */
    @TableField("emp_name")
    private String empName;

    /** 操作类型：CREATE / UPDATE / DELETE / BATCH_CREATE / BATCH_DELETE */
    @TableField("operation")
    private String operation;

    /** 目标类型：TIMESHEET */
    @TableField("target_type")
    private String targetType;

    /** 目标记录ID */
    @TableField("target_id")
    private Long targetId;

    /** 目标日期（工时填报日期） */
    @TableField("target_date")
    private LocalDate targetDate;

    /** 操作详情描述 */
    @TableField("detail")
    private String detail;

    /** 操作时间 */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
