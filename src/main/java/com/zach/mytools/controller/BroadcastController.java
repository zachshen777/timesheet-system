package com.zach.mytools.controller;

import com.zach.mytools.dto.ApiResponse;
import com.zach.mytools.entity.OperationLog;
import com.zach.mytools.service.OperationLogService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 实时广播控制器
 */
@RestController
@RequestMapping("/api/broadcast")
public class BroadcastController {

    private final OperationLogService operationLogService;

    public BroadcastController(OperationLogService operationLogService) {
        this.operationLogService = operationLogService;
    }

    /**
     * 获取最近的广播消息列表
     */
    @GetMapping("/recent")
    public ApiResponse<List<OperationLog>> getRecent(@RequestParam(name = "limit", defaultValue = "10") int limit) {
        List<OperationLog> logs = operationLogService.getRecent(limit);
        return ApiResponse.success("获取成功", logs);
    }
}
