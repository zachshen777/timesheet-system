package com.zach.mytools.dto;

import lombok.Data;

@Data
public class UpdateEmployeeRequest {
    private String name;
    private String workNo;
    private String department;
    private String phone;
    private String email;
    private String role;
}
