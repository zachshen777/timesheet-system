package com.zach.mytools.controller;

import com.zach.mytools.dto.ApiResponse;
import com.zach.mytools.dto.ReportDTO;
import com.zach.mytools.entity.Employee;
import com.zach.mytools.service.AuthService;
import com.zach.mytools.service.ReportService;
import com.zach.mytools.service.TimesheetExportService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

/**
 * 工时报表 REST 控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/report")
public class ReportController {

    private final ReportService reportService;
    private final AuthService authService;
    private final TimesheetExportService exportService;

    public ReportController(ReportService reportService, AuthService authService,
                            TimesheetExportService exportService) {
        this.reportService = reportService;
        this.authService = authService;
        this.exportService = exportService;
    }

    /**
     * 获取当月工时报表（项目汇总 + 明细）
     * GET /api/report/monthly?year=2026&month=7
     */
    @GetMapping("/monthly")
    public ApiResponse<ReportDTO> getMonthlyReport(
            @RequestParam int year,
            @RequestParam int month,
            HttpSession session) {
        Employee employee = authService.getCurrentEmployee(session);
        ReportDTO report = reportService.getMonthlyReport(employee, year, month);
        return ApiResponse.success(report);
    }

    /**
     * 导出当月工时 Excel
     * GET /api/report/export?year=2026&month=7
     */
    @GetMapping("/export")
    public void exportTimesheet(
            @RequestParam int year,
            @RequestParam int month,
            HttpServletResponse response,
            HttpSession session) throws IOException {
        Employee employee = authService.getCurrentEmployee(session);
        log.info("导出工时报表: {} {}-{}", employee.getName(), year, month);
        exportService.export(response, employee, year, month);
    }
}
