package com.zach.mytools.service;

import com.zach.mytools.dto.ReportDTO;
import com.zach.mytools.dto.TimesheetDTO;
import com.zach.mytools.entity.Employee;
import com.zach.mytools.entity.TimesheetRecord;
import com.zach.mytools.mapper.TimesheetRecordMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 工时报表业务服务层
 */
@Slf4j
@Service
public class ReportService {

    private final TimesheetRecordMapper mapper;

    public ReportService(TimesheetRecordMapper mapper) {
        this.mapper = mapper;
    }

    /**
     * 获取指定月份的工时报表数据
     */
    public ReportDTO getMonthlyReport(Employee employee, int year, int month) {
        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());

        List<TimesheetRecord> records = mapper.findByEmpIdAndDateRange(employee.getId(), startDate, endDate);

        // 过滤出有实际工时的记录
        List<TimesheetRecord> filledRecords = records.stream()
                .filter(r -> r.getWorkHours() != null && r.getWorkHours().compareTo(BigDecimal.ZERO) > 0)
                .toList();

        // 计算总工时
        BigDecimal totalHours = filledRecords.stream()
                .map(TimesheetRecord::getWorkHours)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 按项目汇总
        Map<String, BigDecimal> projectMap = new LinkedHashMap<>();
        for (TimesheetRecord r : filledRecords) {
            String project = (r.getProject() != null && !r.getProject().isBlank())
                    ? r.getProject().trim()
                    : "未归类";
            projectMap.merge(project, r.getWorkHours(), BigDecimal::add);
        }

        // 构建项目汇总列表（按工时降序）
        List<ReportDTO.ProjectSummary> summaries = new ArrayList<>();
        BigDecimal finalTotal = totalHours.compareTo(BigDecimal.ZERO) <= 0
                ? BigDecimal.ONE : totalHours;

        projectMap.entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .forEach(entry -> {
                    ReportDTO.ProjectSummary ps = new ReportDTO.ProjectSummary();
                    ps.setProject(entry.getKey());
                    ps.setTotalHours(entry.getValue());
                    ps.setPercentage(entry.getValue()
                            .multiply(BigDecimal.valueOf(100))
                            .divide(finalTotal, 1, RoundingMode.HALF_UP)
                            .doubleValue());
                    summaries.add(ps);
                });

        // 构建明细列表
        List<TimesheetDTO> details = records.stream()
                .filter(r -> r.getWorkHours() != null || r.getOvertimeHours() != null)
                .sorted(Comparator.comparing(TimesheetRecord::getDate))
                .map(this::toDTO)
                .collect(Collectors.toList());

        ReportDTO report = new ReportDTO();
        report.setProjectSummaries(summaries);
        report.setTotalHours(totalHours);
        report.setFilledDays(filledRecords.size());
        report.setDetails(details);
        return report;
    }

    private TimesheetDTO toDTO(TimesheetRecord r) {
        TimesheetDTO dto = new TimesheetDTO();
        dto.setId(r.getId());
        dto.setEmpId(r.getEmpId());
        dto.setDate(r.getDate());
        dto.setProject(r.getProject());
        dto.setTask(r.getTask());
        dto.setWorkHours(r.getWorkHours());
        dto.setOvertimeHours(r.getOvertimeHours());
        dto.setStatus(r.getStatus());
        dto.setCreatedAt(r.getCreatedAt());
        dto.setUpdatedAt(r.getUpdatedAt());
        return dto;
    }
}
