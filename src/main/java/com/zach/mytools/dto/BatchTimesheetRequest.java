package com.zach.mytools.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 批量工时填报请求
 */
@Data
public class BatchTimesheetRequest {
    /** 日期列表（YYYY-MM-DD） */
    private List<String> dates;
    private String project;
    private String task;
    private String remark;
    private BigDecimal workHours;
    private BigDecimal overtimeHours;
    private String status;
}
