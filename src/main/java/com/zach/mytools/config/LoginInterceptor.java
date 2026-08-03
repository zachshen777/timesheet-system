package com.zach.mytools.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zach.mytools.dto.ApiResponse;
import com.zach.mytools.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 登录拦截器：未登录的请求拦截并返回 401 JSON
 */
@Slf4j
@Component
public class LoginInterceptor implements HandlerInterceptor {

    private final AuthService authService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public LoginInterceptor(AuthService authService) {
        this.authService = authService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 放行 OPTIONS 预检请求
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        if (authService.isLoggedIn(request.getSession(false))) {
            return true;
        }
        log.debug("未登录请求拦截: {} {}", request.getMethod(), request.getRequestURI());
        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(401);
        response.getWriter().write(
                objectMapper.writeValueAsString(ApiResponse.error(401, "未登录或登录已过期，请先登录"))
        );
        return false;
    }
}
