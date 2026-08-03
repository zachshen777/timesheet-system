package com.zach.mytools.dto;

import lombok.Data;

/**
 * 部门保存请求 DTO（新增/编辑复用）
 */
@Data
public class DeptSaveRequest {
    /** 部门编码Key（新增时必填，编辑时不可传/忽略） */
    private String dictValue;
    /** 部门名称 */
    private String dictLabel;
    /** 排序 */
    private Integer sort;
    /** 备注 */
    private String remark;
}
