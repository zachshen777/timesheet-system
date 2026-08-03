package com.zach.mytools.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zach.mytools.entity.TimesheetRecord;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 工时填报 Mapper（MyBatis-Plus）
 */
@Mapper
public interface TimesheetRecordMapper extends BaseMapper<TimesheetRecord> {

    /**
     * 根据日期和员工ID查找工时记录
     */
    @Select("SELECT * FROM timesheet_record WHERE date = #{date} AND emp_id = #{empId} LIMIT 1")
    TimesheetRecord findByDateAndEmpId(@Param("date") LocalDate date,
                                       @Param("empId") Long empId);

    /**
     * 查询某员工指定月份的所有工时记录
     */
    @Select("SELECT * FROM timesheet_record WHERE emp_id = #{empId} AND date >= #{startDate} AND date <= #{endDate} ORDER BY date ASC")
    List<TimesheetRecord> findByEmpIdAndDateRange(@Param("empId") Long empId,
                                                    @Param("startDate") LocalDate startDate,
                                                    @Param("endDate") LocalDate endDate);

    /**
     * 根据日期和员工ID删除工时记录
     */
    @Delete("DELETE FROM timesheet_record WHERE date = #{date} AND emp_id = #{empId}")
    int deleteByDateAndEmpId(@Param("date") LocalDate date, @Param("empId") Long empId);

    /**
     * 管理员查询：指定月份全部员工的工时记录（可按姓名模糊筛选、部门精确筛选）
     * JOIN employee 表获取员工姓名
     */
    @Select("<script>" +
            "SELECT t.emp_id, t.date, t.project, t.task, t.remark, t.work_hours, t.overtime_hours, t.status, " +
            "e.name AS employee_name, e.department " +
            "FROM timesheet_record t " +
            "JOIN employee e ON t.emp_id = e.id " +
            "WHERE t.date >= #{startDate} AND t.date &lt;= #{endDate} " +
            "<if test='name != null and name != \"\"'>" +
            "AND e.name LIKE CONCAT('%', #{name}, '%') " +
            "</if>" +
            "<if test='dept != null and dept != \"\"'>" +
            "AND e.department = #{dept} " +
            "</if>" +
            "ORDER BY e.name ASC, t.date ASC" +
            "</script>")
    List<Map<String, Object>> findAllByMonthWithEmployee(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("name") String name,
            @Param("dept") String dept);

    /**
     * 统计某部门下历史工时记录数量
     */
    @Select("SELECT COUNT(*) FROM timesheet_record tr " +
            "JOIN employee e ON tr.emp_id = e.id " +
            "WHERE e.department = #{deptCode}")
    int countByDepartment(@Param("deptCode") String deptCode);
}
