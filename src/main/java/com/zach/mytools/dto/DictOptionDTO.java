package com.zach.mytools.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 字典下拉选项 DTO（供前端 Select 使用）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DictOptionDTO {
    /** 展示文本 */
    private String label;
    /** 编码值 */
    private String value;
}
