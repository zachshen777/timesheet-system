package com.zach.mytools.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 工时填报保存请求
 */
@Data
public class TimesheetSaveRequest {
    /** 填报日期（YYYY-MM-DD） */
    private String date;
    private String project;
    private String task;
    private String remark;
    private BigDecimal workHours;
    private BigDecimal overtimeHours;
    private String status;
}
