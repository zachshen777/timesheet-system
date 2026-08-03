package com.zach.mytools.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zach.mytools.dto.ApiResponse;
import com.zach.mytools.entity.Employee;
import com.zach.mytools.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 管理员权限拦截器：仅 ADMIN 角色可访问 /api/admin/** 路径
 */
@Slf4j
@Component
public class AdminInterceptor implements HandlerInterceptor {

    private final AuthService authService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AdminInterceptor(AuthService authService) {
        this.authService = authService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 放行 OPTIONS 预检请求
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        Employee employee = authService.getCurrentEmployee(request.getSession(false));
        if (!"ADMIN".equals(employee.getRole())) {
            log.warn("非管理员尝试访问管理接口: {} {} {}", employee.getUsername(), request.getMethod(), request.getRequestURI());
            response.setContentType("application/json;charset=UTF-8");
            response.setStatus(403);
            response.getWriter().write(
                    objectMapper.writeValueAsString(ApiResponse.error(403, "无权限，仅管理员可访问"))
            );
            return false;
        }
        return true;
    }
}
