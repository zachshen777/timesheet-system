package com.zach.mytools.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 节假日/调班日项 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HolidayItem {

    /** 名称（春节、国庆、五一调班等） */
    private String name;

    /** 类型：holiday=法定节假日（休息），shift=调班日（上班） */
    private String type;

    /** 开始日期（yyyy-MM-dd） */
    private String startDate;

    /** 结束日期（yyyy-MM-dd） */
    private String endDate;
}
