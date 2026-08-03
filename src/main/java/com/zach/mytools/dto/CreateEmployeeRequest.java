package com.zach.mytools.dto;

import lombok.Data;

@Data
public class CreateEmployeeRequest {
    private String username;
    private String password;
    private String name;
    private String workNo;
    private String department;
    private String phone;
    private String email;
    private String role;  // ADMIN / EMPLOYEE
}
