package com.zach.mytools.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zach.mytools.entity.SysDictItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SysDictItemMapper extends BaseMapper<SysDictItem> {

    /**
     * 根据 dictType 查询所有字典项（按 sort 排序）
     */
    @Select("SELECT * FROM sys_dict_item WHERE dict_type = #{dictType} ORDER BY sort ASC")
    List<SysDictItem> findByDictType(@Param("dictType") String dictType);

    /**
     * 根据 dictType 查询已启用的字典项（按 sort 排序）
     */
    @Select("SELECT * FROM sys_dict_item WHERE dict_type = #{dictType} AND status = 1 ORDER BY sort ASC")
    List<SysDictItem> findEnabledByDictType(@Param("dictType") String dictType);
}
