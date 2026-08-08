package com.zach.mytools.service;

import com.zach.mytools.entity.Employee;
import com.zach.mytools.entity.OperationLog;
import com.zach.mytools.mapper.OperationLogMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 操作日志服务（广播日志）
 */
@Slf4j
@Service
public class OperationLogService {

    private final OperationLogMapper mapper;

    public OperationLogService(OperationLogMapper mapper) {
        this.mapper = mapper;
    }

    /**
     * 记录一条操作日志
     */
    public OperationLog log(Employee employee, String operation, String targetType,
                            Long targetId, LocalDate targetDate, String detail) {
        OperationLog log = new OperationLog();
        log.setEmpId(employee.getId());
        log.setEmpName(employee.getName());
        log.setOperation(operation);
        log.setTargetType(targetType);
        log.setTargetId(targetId);
        log.setTargetDate(targetDate);
        log.setDetail(detail);
        log.setCreatedAt(LocalDateTime.now());
        mapper.insert(log);
        return log;
    }

    /**
     * 查询最近的 N 条广播日志
     */
    public List<OperationLog> getRecent(int limit) {
        return mapper.findRecent(limit);
    }
}
