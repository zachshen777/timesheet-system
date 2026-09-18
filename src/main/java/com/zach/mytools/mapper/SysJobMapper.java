package com.zach.mytools.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zach.mytools.entity.SysJob;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SysJobMapper extends BaseMapper<SysJob> {

    /**
     * 查询所有已启用的任务（服务启动时用于注册调度）
     */
    @Select("SELECT * FROM sys_job WHERE status = 1 ORDER BY id ASC")
    List<SysJob> findEnabled();
}
