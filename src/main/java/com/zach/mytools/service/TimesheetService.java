package com.zach.mytools.service;

import com.zach.mytools.dto.BatchTimesheetRequest;
import com.zach.mytools.dto.TimesheetSaveRequest;
import com.zach.mytools.entity.Employee;
import com.zach.mytools.entity.TimesheetRecord;
import com.zach.mytools.mapper.TimesheetRecordMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
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
    private final OperationLogService operationLogService;

    public TimesheetService(TimesheetRecordMapper mapper, OperationLogService operationLogService) {
        this.mapper = mapper;
        this.operationLogService = operationLogService;
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
     * 查询某员工整年的工时记录（用于热力图）
     */
    public List<TimesheetRecord> getYearRecords(Employee employee, int year) {
        LocalDate startDate = LocalDate.of(year, 1, 1);
        LocalDate endDate = LocalDate.of(year, 12, 31);
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
            logBroadcast(employee, "CREATE", record);
        } else {
            mapper.updateById(record);
            logBroadcast(employee, "UPDATE", record);
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
        TimesheetRecord record = mapper.findByDateAndEmpId(date, employee.getId());
        String detail = buildDetail(record);
        mapper.deleteByDateAndEmpId(date, employee.getId());
        operationLogService.log(employee, "DELETE", "TIMESHEET",
                record != null ? record.getId() : null, date, detail);
    }

    /**
     * 批量删除某员工多个日期的工时记录
     */
    public void batchDeleteByDates(Employee employee, List<LocalDate> dates) {
        for (LocalDate date : dates) {
            deleteByDate(employee, date);
        }
    }

    /**
     * 构建操作详情描述
     */
    private String buildDetail(TimesheetRecord record) {
        if (record == null) return "未知记录";
        BigDecimal total = BigDecimal.ZERO;
        if (record.getWorkHours() != null) total = total.add(record.getWorkHours());
        if (record.getOvertimeHours() != null) total = total.add(record.getOvertimeHours());
        StringBuilder sb = new StringBuilder();
        sb.append(record.getDate()).append(" ");
        if (record.getProject() != null && !record.getProject().isEmpty()) {
            sb.append(record.getProject());
        }
        sb.append(" ").append(total.stripTrailingZeros().toPlainString()).append("h");
        if (record.getTask() != null && !record.getTask().isEmpty()) {
            sb.append(" | ").append(record.getTask());
        }
        return sb.toString();
    }

    /**
     * 记录广播日志
     */
    private void logBroadcast(Employee employee, String operation, TimesheetRecord record) {
        try {
            operationLogService.log(employee, operation, "TIMESHEET",
                    record.getId(), record.getDate(), buildDetail(record));
        } catch (Exception e) {
            log.warn("广播日志记录失败: {}", e.getMessage());
        }
    }
}
