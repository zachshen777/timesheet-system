package com.zach.mytools.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zach.mytools.entity.Employee;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 员工信息 Mapper（MyBatis-Plus）
 */
@Mapper
public interface EmployeeMapper extends BaseMapper<Employee> {

    /**
     * 根据用户名查找员工
     */
    @Select("SELECT * FROM employee WHERE username = #{username} LIMIT 1")
    Employee findByUsername(@Param("username") String username);

    /**
     * 统计某部门下在职员工数量
     */
    @Select("SELECT COUNT(*) FROM employee WHERE department = #{deptCode} AND status = 1")
    int countByDepartment(@Param("deptCode") String deptCode);
}
