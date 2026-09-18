package com.zach.mytools.controller;

import com.zach.mytools.dto.ApiResponse;
import com.zach.mytools.service.SystemConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 系统参数公开查询接口（所有已登录用户可访问）
 * 供前端通用 UI 读取，如下班倒计时悬浮球
 */
@Slf4j
@RestController
@RequestMapping("/api/config")
public class PublicConfigController {

    private final SystemConfigService configService;

    public PublicConfigController(SystemConfigService configService) {
        this.configService = configService;
    }

    /**
     * 获取下班时间（HH:mm）
     * GET /api/config/off-work-time
     */
    @GetMapping("/off-work-time")
    public ApiResponse<String> getOffWorkTime() {
        return ApiResponse.success(configService.getOffWorkTime());
    }
}
