package com.zach.mytools.controller;

import com.zach.mytools.dto.ApiResponse;
import com.zach.mytools.dto.ConfigDTO;
import com.zach.mytools.dto.ConfigRequest;
import com.zach.mytools.dto.HolidayItem;
import com.zach.mytools.entity.SystemConfig;
import com.zach.mytools.entity.Employee;
import com.zach.mytools.service.AuthService;
import com.zach.mytools.service.SystemConfigService;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 系统配置控制器（仅管理员可访问）
 * 管理节假日配置
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/config")
public class SystemConfigController {

    private final SystemConfigService configService;
    private final AuthService authService;

    public SystemConfigController(SystemConfigService configService, AuthService authService) {
        this.configService = configService;
        this.authService = authService;
    }

    /**
     * 获取当前系统配置（含节假日）
     * GET /api/admin/config
     */
    @GetMapping
    public ApiResponse<ConfigDTO> getConfig() {
        SystemConfig config = configService.getConfig();
        List<HolidayItem> holidays = configService.getHolidays();
        return ApiResponse.success(toDTO(config, holidays));
    }

    /**
     * 更新系统配置（节假日）
     * PUT /api/admin/config
     * Body: { "holidays": [{ "name": "春节", "startDate": "2026-02-16", "endDate": "2026-02-22" }] }
     */
    @PutMapping
    public ApiResponse<ConfigDTO> updateConfig(@RequestBody ConfigRequest request, HttpSession session) {
        Employee employee = authService.getCurrentEmployee(session);
        SystemConfig config = configService.saveHolidays(request.getHolidays(), employee.getUsername());
        log.info("系统配置更新: 节假日 {} 条, 操作人: {}", request.getHolidays() != null ? request.getHolidays().size() : 0, employee.getUsername());
        List<HolidayItem> holidays = configService.getHolidays();
        return ApiResponse.success("配置更新成功", toDTO(config, holidays));
    }

    private ConfigDTO toDTO(SystemConfig config, List<HolidayItem> holidays) {
        ConfigDTO dto = new ConfigDTO();
        dto.setHolidays(holidays);
        dto.setUpdatedBy(config.getUpdatedBy());
        return dto;
    }
}
