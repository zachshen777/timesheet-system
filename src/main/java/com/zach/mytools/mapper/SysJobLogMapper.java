package com.zach.mytools.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zach.mytools.entity.SysJobLog;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface SysJobLogMapper extends BaseMapper<SysJobLog> {

    /**
     * 删除某个任务的全部执行日志
     */
    @Delete("DELETE FROM sys_job_log WHERE job_id = #{jobId}")
    int deleteByJobId(@Param("jobId") Long jobId);
}
