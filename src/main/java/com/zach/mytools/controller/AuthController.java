package com.zach.mytools.controller;

import com.zach.mytools.dto.ApiResponse;
import com.zach.mytools.dto.LoginDTO;
import com.zach.mytools.dto.LoginRequest;
import com.zach.mytools.entity.Employee;
import com.zach.mytools.service.AuthService;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 认证控制器（登录 / 退出 / 获取当前用户）
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * 登录
     * POST /api/auth/login
     * Body: { "username": "admin", "password": "admin123" }
     */
    @PostMapping("/login")
    public ApiResponse<LoginDTO> login(@RequestBody LoginRequest request, HttpSession session) {
        if (request.getUsername() == null || request.getUsername().isBlank()) {
            return ApiResponse.error(400, "用户名不能为空");
        }
        if (request.getPassword() == null || request.getPassword().isBlank()) {
            return ApiResponse.error(400, "密码不能为空");
        }
        try {
            Employee emp = authService.login(request.getUsername(), request.getPassword(), session);
            log.info("用户登录成功: {} ({})", emp.getUsername(), emp.getName());
            return ApiResponse.success("登录成功", toDTO(emp));
        } catch (IllegalArgumentException | IllegalStateException e) {
            log.warn("用户登录失败 [{}]: {}", request.getUsername(), e.getMessage());
            return ApiResponse.error(401, e.getMessage());
        }
    }

    /**
     * 退出登录
     * POST /api/auth/logout
     */
    @PostMapping("/logout")
    public ApiResponse<Void> logout(HttpSession session) {
        Employee emp = authService.getCurrentEmployee(session);
        log.info("用户退出登录: {} ({})", emp.getUsername(), emp.getName());
        authService.logout(session);
        return ApiResponse.success("退出登录成功", null);
    }

    /**
     * 获取当前登录用户信息
     * GET /api/auth/me
     */
    @GetMapping("/me")
    public ApiResponse<LoginDTO> currentUser(HttpSession session) {
        try {
            Employee emp = authService.getCurrentEmployee(session);
            return ApiResponse.success(toDTO(emp));
        } catch (IllegalStateException e) {
            return ApiResponse.error(401, e.getMessage());
        }
    }

    /**
     * Entity 转 DTO（隐藏密码字段）
     */
    private LoginDTO toDTO(Employee emp) {
        LoginDTO dto = new LoginDTO();
        dto.setId(emp.getId());
        dto.setUsername(emp.getUsername());
        dto.setName(emp.getName());
        dto.setWorkNo(emp.getWorkNo());
        dto.setDepartment(emp.getDepartment());
        dto.setPhone(emp.getPhone());
        dto.setRole(emp.getRole());
        return dto;
    }
}
