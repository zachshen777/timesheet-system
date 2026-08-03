package com.zach.mytools.service;

import com.zach.mytools.entity.Employee;
import com.zach.mytools.mapper.EmployeeMapper;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * 认证服务层
 */
@Slf4j
@Service
public class AuthService {

    private final EmployeeMapper employeeMapper;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    /** Session 中存储当前登录员工的 Key */
    public static final String SESSION_KEY = "currentEmployee";

    public AuthService(EmployeeMapper employeeMapper) {
        this.employeeMapper = employeeMapper;
    }

    /**
     * 登录验证
     */
    public Employee login(String username, String password, HttpSession session) {
        Employee employee = employeeMapper.findByUsername(username);
        if (employee == null) {
            throw new IllegalArgumentException("用户名不存在");
        }
        if (employee.getStatus() != null && employee.getStatus() != 1) {
            log.warn("账号已被禁用尝试登录: {}", username);
            throw new IllegalStateException("账号已被禁用");
        }
        if (!passwordEncoder.matches(password, employee.getPassword())) {
            throw new IllegalArgumentException("密码错误");
        }
        session.setAttribute(SESSION_KEY, employee);
        return employee;
    }

    /**
     * 退出登录
     */
    public void logout(HttpSession session) {
        session.invalidate();
    }

    /**
     * 从 Session 获取当前登录员工（未登录则抛异常）
     */
    public Employee getCurrentEmployee(HttpSession session) {
        if (session == null) {
            throw new IllegalStateException("未登录或登录已过期");
        }
        Object emp = session.getAttribute(SESSION_KEY);
        if (emp == null) {
            throw new IllegalStateException("未登录或登录已过期");
        }
        return (Employee) emp;
    }

    /**
     * 判断当前 Session 是否已登录
     */
    public boolean isLoggedIn(HttpSession session) {
        if (session == null) {
            return false;
        }
        return session.getAttribute(SESSION_KEY) != null;
    }
}
