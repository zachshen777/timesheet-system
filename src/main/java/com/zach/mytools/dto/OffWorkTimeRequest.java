package com.zach.mytools.dto;

import lombok.Data;

/**
 * 下班时间更新请求 DTO
 */
@Data
public class OffWorkTimeRequest {

    /** 下班时间（HH:mm，如 17:00） */
    private String offWorkTime;
}
