package com.zach.mytools.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 字典数据项实体
 */
@Data
@TableName("sys_dict_item")
public class SysDictItem {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 字典类型编码（如 system_dept） */
    @TableField("dict_type")
    private String dictType;

    /** 展示名称（部门名称） */
    @TableField("dict_label")
    private String dictLabel;

    /** 唯一编码Key（部门编码，英文/数字） */
    @TableField("dict_value")
    private String dictValue;

    /** 排序（升序） */
    @TableField("sort")
    private Integer sort;

    /** 状态：1启用 0禁用 */
    @TableField("status")
    private Integer status;

    /** 备注 */
    @TableField("remark")
    private String remark;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
