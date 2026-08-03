package com.zach.mytools.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class EmployeeInfoDTO {
    private Long id;
    private String username;
    private String name;
    private String workNo;
    private String department;
    private String phone;
    private String email;
    private String role;
    private Integer status;       // 1启用 0禁用
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
