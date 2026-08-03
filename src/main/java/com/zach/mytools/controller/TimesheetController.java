package com.zach.mytools.controller;

import com.zach.mytools.dto.ApiResponse;
import com.zach.mytools.dto.BatchTimesheetRequest;
import com.zach.mytools.dto.TimesheetDTO;
import com.zach.mytools.dto.TimesheetSaveRequest;
import com.zach.mytools.entity.Employee;
import com.zach.mytools.entity.TimesheetRecord;
import com.zach.mytools.service.AuthService;
import com.zach.mytools.service.TimesheetService;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 工时填报 REST 控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/timesheet")
public class TimesheetController {

    private final TimesheetService timesheetService;
    private final AuthService authService;

    public TimesheetController(TimesheetService timesheetService, AuthService authService) {
        this.timesheetService = timesheetService;
        this.authService = authService;
    }

    /**
     * 查询当月工时记录
     * GET /api/timesheet/month?year=2026&month=7
     * 返回 { "2026-07-01": { ... }, "2026-07-02": { ... } } 格式的 Map
     */
    @GetMapping("/month")
    public ApiResponse<Map<String, TimesheetDTO>> getMonthRecords(
            @RequestParam int year,
            @RequestParam int month,
            HttpSession session) {
        Employee employee = authService.getCurrentEmployee(session);
        List<TimesheetRecord> records = timesheetService.getMonthRecords(employee, year, month);
        Map<String, TimesheetDTO> map = records.stream()
                .collect(Collectors.toMap(
                        r -> r.getDate().toString(),
                        this::toDTO
                ));
        return ApiResponse.success(map);
    }

    /**
     * 查询指定日期的工时记录
     * GET /api/timesheet/date/{date}
     */
    @GetMapping("/date/{date}")
    public ApiResponse<TimesheetDTO> getByDate(@PathVariable String date, HttpSession session) {
        Employee employee = authService.getCurrentEmployee(session);
        TimesheetRecord record = timesheetService.getByDate(employee, LocalDate.parse(date));
        if (record == null) {
            return ApiResponse.success(null);
        }
        return ApiResponse.success(toDTO(record));
    }

    /**
     * 保存（新增或更新）工时记录
     * POST /api/timesheet/save
     */
    @PostMapping("/save")
    public ApiResponse<TimesheetDTO> save(@RequestBody TimesheetSaveRequest request,
                                          HttpSession session) {
        Employee employee = authService.getCurrentEmployee(session);
        try {
            TimesheetRecord record = timesheetService.save(employee, request);
            log.info("工时保存成功: {} {} {} {}h", employee.getName(), request.getDate(),
                    request.getProject(), request.getWorkHours());
            return ApiResponse.success("保存成功", toDTO(record));
        } catch (IllegalArgumentException e) {
            log.warn("工时保存失败 [{}]: {}", request.getDate(), e.getMessage());
            return ApiResponse.error(400, e.getMessage());
        }
    }

    /**
     * 批量保存工时记录
     * POST /api/timesheet/batch
     */
    @PostMapping("/batch")
    public ApiResponse<List<TimesheetDTO>> batchSave(@RequestBody BatchTimesheetRequest request,
                                                      HttpSession session) {
        Employee employee = authService.getCurrentEmployee(session);
        try {
            List<TimesheetRecord> records = timesheetService.batchSave(employee, request);
            List<TimesheetDTO> dtos = records.stream().map(this::toDTO).toList();
            log.info("批量工时保存成功: {} {} 条记录 [{}] {}h", employee.getName(),
                    dtos.size(), request.getProject(), request.getWorkHours());
            return ApiResponse.success("批量保存成功（" + dtos.size() + " 条）", dtos);
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }

    /**
     * 删除指定日期的工时记录
     * DELETE /api/timesheet/delete/{date}
     */
    @DeleteMapping("/delete/{date}")
    public ApiResponse<Void> delete(@PathVariable String date, HttpSession session) {
        Employee employee = authService.getCurrentEmployee(session);
        timesheetService.deleteByDate(employee, LocalDate.parse(date));
        log.info("工时删除: {} {}", employee.getName(), date);
        return ApiResponse.success("删除成功", null);
    }

    /**
     * 批量删除工时记录
     * DELETE /api/timesheet/batch-delete
     */
    @DeleteMapping("/batch-delete")
    public ApiResponse<Void> batchDelete(@RequestBody Map<String, List<String>> body,
                                          HttpSession session) {
        Employee employee = authService.getCurrentEmployee(session);
        List<LocalDate> dates = body.get("dates").stream()
                .map(LocalDate::parse).toList();
        timesheetService.batchDeleteByDates(employee, dates);
        return ApiResponse.success("批量删除成功", null);
    }

    /**
     * Entity 转 DTO
     */
    private TimesheetDTO toDTO(TimesheetRecord r) {
        TimesheetDTO dto = new TimesheetDTO();
        dto.setId(r.getId());
        dto.setEmpId(r.getEmpId());
        dto.setDate(r.getDate());
        dto.setProject(r.getProject());
        dto.setTask(r.getTask());
        dto.setRemark(r.getRemark());
        dto.setWorkHours(r.getWorkHours());
        dto.setOvertimeHours(r.getOvertimeHours());
        dto.setStatus(r.getStatus());
        dto.setCreatedAt(r.getCreatedAt());
        dto.setUpdatedAt(r.getUpdatedAt());
        return dto;
    }
}
