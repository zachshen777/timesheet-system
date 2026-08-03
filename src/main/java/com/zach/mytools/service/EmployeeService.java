package com.zach.mytools.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zach.mytools.entity.Employee;
import com.zach.mytools.mapper.EmployeeMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Slf4j
@Service
public class EmployeeService {

    private final EmployeeMapper employeeMapper;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public EmployeeService(EmployeeMapper employeeMapper) {
        this.employeeMapper = employeeMapper;
    }

    /** 获取所有员工 */
    public List<Employee> listAll() {
        return employeeMapper.selectList(null);
    }

    /**
     * 分页查询员工（支持多条件筛选）
     * @param deptCode  部门编码（可选）
     * @param username  用户名模糊搜索（可选）
     * @param name      姓名模糊搜索（可选）
     * @param status    状态筛选（可选：1启用 0禁用）
     */
    public IPage<Employee> listByPage(int page, int size,
                                       String deptCode, String username,
                                       String name, Integer status) {
        Page<Employee> p = new Page<>(page, size);
        LambdaQueryWrapper<Employee> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(deptCode)) {
            wrapper.eq(Employee::getDepartment, deptCode);
        }
        if (StringUtils.hasText(username)) {
            wrapper.like(Employee::getUsername, username);
        }
        if (StringUtils.hasText(name)) {
            wrapper.like(Employee::getName, name);
        }
        if (status != null) {
            wrapper.eq(Employee::getStatus, status);
        }
        wrapper.orderByAsc(Employee::getId);
        return employeeMapper.selectPage(p, wrapper);
    }

    /** 新增员工 */
    public Employee create(String username, String password, String name,
                           String workNo, String department, String phone,
                           String email, String role) {
        // 校验必填
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("用户名不能为空");
        }
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("密码不能为空");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("姓名不能为空");
        }

        // 检查用户名唯一
        Employee existing = employeeMapper.findByUsername(username);
        if (existing != null) {
            throw new IllegalStateException("用户名已存在");
        }

        Employee employee = new Employee();
        employee.setUsername(username);
        employee.setPassword(passwordEncoder.encode(password));
        employee.setName(name);
        employee.setWorkNo(workNo);
        employee.setDepartment(department);
        employee.setPhone(phone);
        employee.setEmail(email);
        employee.setRole(role != null ? role : "EMPLOYEE");
        employee.setStatus(1);
        employeeMapper.insert(employee);
        return employee;
    }

    /** 更新员工信息 */
    public Employee update(Long id, String name, String workNo,
                           String department, String phone, String email, String role) {
        Employee employee = employeeMapper.selectById(id);
        if (employee == null) {
            throw new IllegalArgumentException("员工不存在");
        }

        employee.setName(name);
        employee.setWorkNo(workNo);
        employee.setDepartment(department);
        employee.setPhone(phone);
        employee.setEmail(email);
        if (role != null) {
            employee.setRole(role);
        }
        employeeMapper.updateById(employee);
        return employee;
    }

    /** 切换状态（启用/禁用） */
    public Employee toggleStatus(Long id) {
        Employee employee = employeeMapper.selectById(id);
        if (employee == null) {
            throw new IllegalArgumentException("员工不存在");
        }
        employee.setStatus(employee.getStatus() == 1 ? 0 : 1);
        employeeMapper.updateById(employee);
        return employee;
    }

    /** 重置密码 */
    public void resetPassword(Long id, String newPassword) {
        Employee employee = employeeMapper.selectById(id);
        if (employee == null) {
            throw new IllegalArgumentException("员工不存在");
        }
        if (newPassword == null || newPassword.isBlank()) {
            throw new IllegalArgumentException("密码不能为空");
        }
        employee.setPassword(passwordEncoder.encode(newPassword));
        employeeMapper.updateById(employee);
    }
}
