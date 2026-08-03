package com.zach.mytools.controller;

import com.zach.mytools.dto.ApiResponse;
import com.zach.mytools.dto.DeptSaveRequest;
import com.zach.mytools.dto.DictOptionDTO;
import com.zach.mytools.entity.SysDictItem;
import com.zach.mytools.service.SysDictService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 部门管理控制器
 * /api/admin/dept/* — 管理员操作
 * /api/common/dept/options — 公共下拉
 */
@Slf4j
@RestController
public class SysDictController {

    private final SysDictService dictService;

    public SysDictController(SysDictService dictService) {
        this.dictService = dictService;
    }

    // ==================== 公共下拉（所有已登录用户） ====================

    /**
     * GET /api/common/dept/options
     * 返回 [{label, value}]，仅启用项，按 sort 排序
     */
    @GetMapping("/api/common/dept/options")
    public ApiResponse<List<DictOptionDTO>> getDeptOptions() {
        return ApiResponse.success(dictService.getDeptOptions());
    }

    // ==================== 管理员操作 ====================

    /**
     * GET /api/admin/dept/list — 查询所有部门（含禁用）
     */
    @GetMapping("/api/admin/dept/list")
    public ApiResponse<List<SysDictItem>> listAll() {
        return ApiResponse.success(dictService.listAll());
    }

    /**
     * POST /api/admin/dept — 新增部门
     */
    @PostMapping("/api/admin/dept")
    public ApiResponse<SysDictItem> create(@RequestBody DeptSaveRequest request) {
        try {
            SysDictItem item = dictService.create(request);
            return ApiResponse.success("部门创建成功", item);
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }

    /**
     * PUT /api/admin/dept/{id} — 编辑部门（dictValue 不可改）
     */
    @PutMapping("/api/admin/dept/{id}")
    public ApiResponse<SysDictItem> update(@PathVariable Long id,
                                           @RequestBody DeptSaveRequest request) {
        try {
            SysDictItem item = dictService.update(id, request);
            return ApiResponse.success("部门更新成功", item);
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }

    /**
     * PUT /api/admin/dept/{id}/status — 启用/禁用
     */
    @PutMapping("/api/admin/dept/{id}/status")
    public ApiResponse<SysDictItem> toggleStatus(@PathVariable Long id) {
        try {
            SysDictItem item = dictService.toggleStatus(id);
            String msg = item.getStatus() == 1 ? "部门已启用" : "部门已禁用";
            return ApiResponse.success(msg, item);
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }

    /**
     * DELETE /api/admin/dept/{id} — 删除部门（前置校验）
     */
    @DeleteMapping("/api/admin/dept/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        try {
            dictService.delete(id);
            return ApiResponse.success("部门已删除", null);
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(400, e.getMessage());
        } catch (IllegalStateException e) {
            return ApiResponse.error(409, e.getMessage());
        }
    }
}
