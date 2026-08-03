package com.zach.mytools.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 工时填报 DTO
 */
@Data
public class TimesheetDTO {
    private Long id;
    private Long empId;
    private LocalDate date;
    private String project;
    private String task;
    private String remark;
    private BigDecimal workHours;
    private BigDecimal overtimeHours;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
