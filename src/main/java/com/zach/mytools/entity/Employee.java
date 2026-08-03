package com.zach.mytools.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 员工信息实体
 */
@Data
@TableName("employee")
public class Employee {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 登录用户名 */
    @TableField("username")
    private String username;

    /** 密码（BCrypt 加密） */
    @TableField("password")
    private String password;

    /** 员工姓名 */
    @TableField("name")
    private String name;

    /** 工号 */
    @TableField("work_no")
    private String workNo;

    /** 部门 */
    @TableField("department")
    private String department;

    /** 手机号 */
    @TableField("phone")
    private String phone;

    /** 邮箱 */
    @TableField("email")
    private String email;

    /** 角色：ADMIN-管理员 EMPLOYEE-普通员工 */
    @TableField("role")
    private String role;

    /** 状态：1启用 0禁用 */
    @TableField("status")
    private Integer status;

    /** 创建时间（插入时自动填充） */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /** 更新时间（插入和更新时自动填充） */
    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
