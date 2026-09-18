package com.zach.mytools.job.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zach.mytools.entity.TimesheetRecord;
import com.zach.mytools.job.ScheduledJob;
import com.zach.mytools.mapper.TimesheetRecordMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * 示例任务：草稿工时统计
 * <p>
 * 演示「任务类里直接注入 Mapper 查库」的写法（本类注册为 Spring Bean，调度器会优先复用容器实例，
 * 因此依赖注入正常生效）。统计当月仍处于 DRAFT（草稿）状态的填报记录数并打日志。
 * <p>
 * 在「系统设置 → 定时任务」里把执行类填 com.zach.mytools.job.task.MonthlyDraftStatJob 即可。
 */
@Slf4j
@Component
public class MonthlyDraftStatJob implements ScheduledJob {

    private final TimesheetRecordMapper timesheetRecordMapper;

    public MonthlyDraftStatJob(TimesheetRecordMapper timesheetRecordMapper) {
        this.timesheetRecordMapper = timesheetRecordMapper;
    }

    @Override
    public void execute() {
        LocalDate today = LocalDate.now();
        LocalDate firstDay = today.withDayOfMonth(1);
        LocalDate lastDay = today.withDayOfMonth(today.lengthOfMonth());

        Long draftCount = timesheetRecordMapper.selectCount(
                new LambdaQueryWrapper<TimesheetRecord>()
                        .eq(TimesheetRecord::getStatus, "DRAFT")
                        .between(TimesheetRecord::getDate, firstDay, lastDay)
        );

        log.info("[草稿统计] {} 年月（{} ~ {}）未提交的草稿记录共 {} 条",
                today.getYear(), firstDay, lastDay, draftCount == null ? 0 : draftCount);
    }
}
