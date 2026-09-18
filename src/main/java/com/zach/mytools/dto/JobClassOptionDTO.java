package com.zach.mytools.dto;

import lombok.Data;

/**
 * 可选任务类下拉项（用于前端「执行类」选择框的候选列表）
 */
@Data
public class JobClassOptionDTO {

    /** 全限定类名 */
    private String className;

    /** 展示名称（简单类名） */
    private String label;

    /** 来源：SPRING_BEAN-容器中的任务Bean */
    private String source;

    public JobClassOptionDTO() {
    }

    public JobClassOptionDTO(String className, String label, String source) {
        this.className = className;
        this.label = label;
        this.source = source;
    }
}
