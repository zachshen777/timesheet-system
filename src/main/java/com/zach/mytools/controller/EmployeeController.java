package com.zach.mytools.controller;

import com.zach.mytools.dto.*;
import com.zach.mytools.entity.Employee;
import com.zach.mytools.service.EmployeeService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 员工管理控制器（仅管理员可访问）
 * 管理员工信息：列表查询、新增、编辑、删除、启用/禁用、重置密码
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/employees")
public class EmployeeController {

    private final EmployeeService employeeService;

    public EmployeeController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    /**
     * 分页查询员工列表（支持多条件筛选）
     * GET /api/admin/employees?page=1&size=10&dept=technology&username=zhang&name=张三&status=1
     */
    @GetMapping
    public ApiResponse<PageResult<EmployeeInfoDTO>> listAll(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String dept,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Integer status) {
        IPage<Employee> result = employeeService.listByPage(page, size, dept, username, name, status);
        List<EmployeeInfoDTO> records = result.getRecords().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
        return ApiResponse.success(new PageResult<>(records, result.getTotal()));
    }

    /**
     * 新增员工
     * POST /api/admin/employees
     */
    @PostMapping
    public ApiResponse<EmployeeInfoDTO> create(@RequestBody CreateEmployeeRequest request) {
        try {
            Employee employee = employeeService.create(
                    request.getUsername(),
                    request.getPassword(),
                    request.getName(),
                    request.getWorkNo(),
                    request.getDepartment(),
                    request.getPhone(),
                    request.getEmail(),
                    request.getRole()
            );
            log.info("员工创建成功: {} ({}) 部门: {}", employee.getUsername(), employee.getName(), employee.getDepartment());
            return ApiResponse.success("员工创建成功", toDTO(employee));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }

    /**
     * 更新员工信息
     * PUT /api/admin/employees/{id}
     */
    @PutMapping("/{id}")
    public ApiResponse<EmployeeInfoDTO> update(@PathVariable Long id,
                                               @RequestBody UpdateEmployeeRequest request) {
        try {
            Employee employee = employeeService.update(
                    id,
                    request.getName(),
                    request.getWorkNo(),
                    request.getDepartment(),
                    request.getPhone(),
                    request.getEmail(),
                    request.getRole()
            );
            return ApiResponse.success("员工信息更新成功", toDTO(employee));
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }

    /**
     * 切换员工状态（启用/禁用）
     * PUT /api/admin/employees/{id}/status
     */
    @PutMapping("/{id}/status")
    public ApiResponse<EmployeeInfoDTO> toggleStatus(@PathVariable Long id) {
        try {
            Employee employee = employeeService.toggleStatus(id);
            String msg = employee.getStatus() == 1 ? "员工已启用" : "员工已禁用";
            return ApiResponse.success(msg, toDTO(employee));
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }

    /**
     * 重置员工密码
     * PUT /api/admin/employees/{id}/reset-password
     */
    @PutMapping("/{id}/reset-password")
    public ApiResponse<Void> resetPassword(@PathVariable Long id,
                                           @RequestBody ResetPasswordRequest request) {
        try {
            employeeService.resetPassword(id, request.getNewPassword());
            return ApiResponse.success("密码重置成功", null);
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }

    private EmployeeInfoDTO toDTO(Employee employee) {
        EmployeeInfoDTO dto = new EmployeeInfoDTO();
        dto.setId(employee.getId());
        dto.setUsername(employee.getUsername());
        dto.setName(employee.getName());
        dto.setWorkNo(employee.getWorkNo());
        dto.setDepartment(employee.getDepartment());
        dto.setPhone(employee.getPhone());
        dto.setEmail(employee.getEmail());
        dto.setRole(employee.getRole());
        dto.setStatus(employee.getStatus());
        dto.setCreatedAt(employee.getCreatedAt());
        dto.setUpdatedAt(employee.getUpdatedAt());
        return dto;
    }
}
