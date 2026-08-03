package com.zach.mytools.dto;

import lombok.Data;

/**
 * 登录响应 DTO（不含密码）
 */
@Data
public class LoginDTO {

    private Long id;
    private String username;
    private String name;
    private String workNo;
    private String department;
    private String phone;
    private String role;   // ADMIN / EMPLOYEE
}
