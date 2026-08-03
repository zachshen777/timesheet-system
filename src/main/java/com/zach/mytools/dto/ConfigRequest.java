package com.zach.mytools.dto;

import lombok.Data;

import java.util.List;

/**
 * 系统配置更新请求 DTO
 */
@Data
public class ConfigRequest {

    /** 节假日列表 */
    private List<HolidayItem> holidays;
}
