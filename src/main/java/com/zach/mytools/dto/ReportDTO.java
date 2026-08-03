package com.zach.mytools.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 工时报表 DTO
 */
@Data
public class ReportDTO {

    /** 项目工时汇总 */
    private List<ProjectSummary> projectSummaries;

    /** 本月总工时 */
    private BigDecimal totalHours;

    /** 本月已填报天数 */
    private int filledDays;

    /** 工时明细列表 */
    private List<TimesheetDTO> details;

    /**
     * 单项项目工时汇总
     */
    @Data
    public static class ProjectSummary {
        /** 项目名称 */
        private String project;
        /** 累计工时 */
        private BigDecimal totalHours;
        /** 占比（百分比数值，如 45.5 表示 45.5%） */
        private double percentage;
    }
}
