package com.zach.mytools.dto;

import lombok.Data;

import java.util.List;

/**
 * 系统配置响应 DTO
 */
@Data
public class ConfigDTO {

    /** 节假日列表 */
    private List<HolidayItem> holidays;

    /** 最后修改人 */
    private String updatedBy;
}
