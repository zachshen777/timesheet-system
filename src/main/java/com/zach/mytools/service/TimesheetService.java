package com.zach.mytools.service;

import com.zach.mytools.dto.BatchTimesheetRequest;
import com.zach.mytools.dto.TimesheetSaveRequest;
import com.zach.mytools.entity.Employee;
import com.zach.mytools.entity.TimesheetRecord;
import com.zach.mytools.mapper.TimesheetRecordMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 工时填报业务服务层
 */
@Slf4j
@Service
public class TimesheetService {

    private final TimesheetRecordMapper mapper;

    public TimesheetService(TimesheetRecordMapper mapper) {
        this.mapper = mapper;
    }

    /**
     * 查询某员工指定月份的工时记录
     */
    public List<TimesheetRecord> getMonthRecords(Employee employee, int year, int month) {
        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());
        return mapper.findByEmpIdAndDateRange(employee.getId(), startDate, endDate);
    }

    /**
     * 查询某员工指定日期的工时记录
     */
    public TimesheetRecord getByDate(Employee employee, LocalDate date) {
        return mapper.findByDateAndEmpId(date, employee.getId());
    }

    /**
     * 保存（新增或更新）工时记录
     */
    public TimesheetRecord save(Employee employee, TimesheetSaveRequest req) {
        if (req.getDate() == null || req.getDate().isBlank()) {
            throw new IllegalArgumentException("填报日期不能为空");
        }

        LocalDate date = LocalDate.parse(req.getDate());
        TimesheetRecord record = mapper.findByDateAndEmpId(date, employee.getId());
        boolean isNew = (record == null);

        if (isNew) {
            record = new TimesheetRecord();
            record.setEmpId(employee.getId());
            record.setDate(date);
        }

        record.setProject(req.getProject());
        record.setTask(req.getTask());
        record.setRemark(req.getRemark());
        record.setWorkHours(req.getWorkHours());
        record.setOvertimeHours(req.getOvertimeHours());
        record.setStatus(req.getStatus() != null ? req.getStatus() : "DRAFT");

        if (isNew) {
            mapper.insert(record);
        } else {
            mapper.updateById(record);
        }
        return record;
    }

    /**
     * 批量保存工时记录（对多个日期填入相同内容）
     */
    public List<TimesheetRecord> batchSave(Employee employee, BatchTimesheetRequest req) {
        if (req.getDates() == null || req.getDates().isEmpty()) {
            throw new IllegalArgumentException("日期列表不能为空");
        }

        List<TimesheetRecord> results = new ArrayList<>();
        for (String dateStr : req.getDates()) {
            TimesheetSaveRequest single = new TimesheetSaveRequest();
            single.setDate(dateStr);
            single.setProject(req.getProject());
            single.setTask(req.getTask());
            single.setRemark(req.getRemark());
            single.setWorkHours(req.getWorkHours());
            single.setOvertimeHours(req.getOvertimeHours());
            single.setStatus(req.getStatus() != null ? req.getStatus() : "DRAFT");
            results.add(save(employee, single));
        }
        return results;
    }

    /**
     * 删除某员工指定日期的工时记录
     */
    public void deleteByDate(Employee employee, LocalDate date) {
        mapper.deleteByDateAndEmpId(date, employee.getId());
    }

    /**
     * 批量删除某员工多个日期的工时记录
     */
    public void batchDeleteByDates(Employee employee, List<LocalDate> dates) {
        for (LocalDate date : dates) {
            mapper.deleteByDateAndEmpId(date, employee.getId());
        }
    }
}
