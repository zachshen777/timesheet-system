package com.zach.mytools.controller;

import com.zach.mytools.dto.*;
import com.zach.mytools.entity.SysJob;
import com.zach.mytools.entity.SysJobLog;
import com.zach.mytools.service.AuthService;
import com.zach.mytools.service.SysJobService;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 定时任务管理控制器（仅管理员可访问，由 AdminInterceptor 统一拦截 /api/admin/**）
 * <ul>
 *     <li>GET    /api/admin/job/list          任务列表</li>
 *     <li>POST   /api/admin/job               新增任务</li>
 *     <li>PUT    /api/admin/job/{id}          编辑任务</li>
 *     <li>PUT    /api/admin/job/{id}/status   启用/禁用</li>
 *     <li>POST   /api/admin/job/{id}/run      立即执行一次</li>
 *     <li>DELETE /api/admin/job/{id}          删除任务</li>
 *     <li>GET    /api/admin/job/logs          执行日志分页查询</li>
 *     <li>DELETE /api/admin/job/logs          清理执行日志</li>
 *     <li>GET    /api/admin/job/classes       候选执行类</li>
 * </ul>
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/job")
public class SysJobController {

    private final SysJobService jobService;
    private final AuthService authService;

    public SysJobController(SysJobService jobService, AuthService authService) {
        this.jobService = jobService;
        this.authService = authService;
    }

    /**
     * 任务列表（含「下次执行时间」「是否运行中」等运行时状态）
     */
    @GetMapping("/list")
    public ApiResponse<List<JobInfoDTO>> list() {
        return ApiResponse.success(jobService.listAll());
    }

    /**
     * 候选执行类（容器中的 ScheduledJob 实现），供前端下拉选择
     */
    @GetMapping("/classes")
    public ApiResponse<List<JobClassOptionDTO>> classes() {
        return ApiResponse.success(jobService.listCandidateClasses());
    }

    /**
     * 新增任务
     */
    @PostMapping
    public ApiResponse<SysJob> create(@RequestBody JobSaveRequest request, HttpSession session) {
        try {
            SysJob job = jobService.create(request, currentUsername(session));
            String msg = job.getStatus() != null && job.getStatus() == 1
                    ? "任务创建成功，已开始调度" : "任务创建成功（当前为禁用状态）";
            return ApiResponse.success(msg, job);
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }

    /**
     * 编辑任务（保存后立即按新配置重新调度）
     */
    @PutMapping("/{id}")
    public ApiResponse<SysJob> update(@PathVariable Long id, @RequestBody JobSaveRequest request) {
        try {
            SysJob job = jobService.update(id, request);
            return ApiResponse.success("任务更新成功，已按新配置生效", job);
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }

    /**
     * 启用/禁用
     */
    @PutMapping("/{id}/status")
    public ApiResponse<SysJob> toggleStatus(@PathVariable Long id) {
        try {
            SysJob job = jobService.toggleStatus(id);
            String msg = job.getStatus() != null && job.getStatus() == 1 ? "任务已启用并开始调度" : "任务已禁用";
            return ApiResponse.success(msg, job);
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }

    /**
     * 立即执行一次（不影响原有调度计划）
     */
    @PostMapping("/{id}/run")
    public ApiResponse<Void> runOnce(@PathVariable Long id) {
        try {
            jobService.triggerOnce(id);
            return ApiResponse.success("已触发执行，稍后可在「执行日志」中查看结果", null);
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }

    /**
     * 删除任务（执行日志保留）
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        try {
            jobService.delete(id);
            return ApiResponse.success("任务已删除", null);
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }

    /**
     * 执行日志分页查询
     *
     * @param startDate 起始日期（yyyy-MM-dd）
     * @param endDate   结束日期（yyyy-MM-dd）
     */
    @GetMapping("/logs")
    public ApiResponse<PageResult<SysJobLog>> logs(@RequestParam(defaultValue = "1") int page,
                                                  @RequestParam(defaultValue = "10") int size,
                                                  @RequestParam(required = false) Long jobId,
                                                  @RequestParam(required = false) String status,
                                                  @RequestParam(required = false) String startDate,
                                                  @RequestParam(required = false) String endDate) {
        try {
            return ApiResponse.success(jobService.pageLogs(page, size, jobId, status, startDate, endDate));
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }

    /**
     * 清理执行日志（不传 jobId 表示清空全部）
     */
    @DeleteMapping("/logs")
    public ApiResponse<Integer> clearLogs(@RequestParam(required = false) Long jobId) {
        try {
            int deleted = jobService.clearLogs(jobId);
            return ApiResponse.success("已清理 " + deleted + " 条执行日志", deleted);
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }

    private String currentUsername(HttpSession session) {
        try {
            return authService.getCurrentEmployee(session).getUsername();
        } catch (Exception e) {
            return "unknown";
        }
    }
}
