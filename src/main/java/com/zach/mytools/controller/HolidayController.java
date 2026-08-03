package com.zach.mytools.controller;

import com.zach.mytools.dto.ApiResponse;
import com.zach.mytools.dto.HolidayItem;
import com.zach.mytools.service.SystemConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 节假日公开查询接口（所有已登录用户可访问）
 */
@Slf4j
@RestController
@RequestMapping("/api/holidays")
public class HolidayController {

    private final SystemConfigService configService;

    public HolidayController(SystemConfigService configService) {
        this.configService = configService;
    }

    /**
     * 获取节假日列表（所有登录用户均可访问）
     * GET /api/holidays
     */
    @GetMapping
    public ApiResponse<List<HolidayItem>> getHolidays() {
        List<HolidayItem> holidays = configService.getHolidays();
        return ApiResponse.success(holidays);
    }
}
