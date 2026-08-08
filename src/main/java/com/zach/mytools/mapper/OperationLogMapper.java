package com.zach.mytools.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zach.mytools.entity.OperationLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 操作日志 Mapper
 */
@Mapper
public interface OperationLogMapper extends BaseMapper<OperationLog> {

    /**
     * 查询最近的 N 条操作日志
     */
    @Select("SELECT * FROM operation_log ORDER BY created_at DESC LIMIT #{limit}")
    List<OperationLog> findRecent(int limit);
}
