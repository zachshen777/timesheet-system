package com.zach.mytools.controller;

import com.zach.mytools.dto.ApiResponse;
import com.zach.mytools.dto.HolidayItem;
import com.zach.mytools.mapper.TimesheetRecordMapper;
import com.zach.mytools.service.SystemConfigService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.*;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 管理员工时历史查询控制器（/api/admin/timesheet）
 * 查询所有员工的工时记录，支持按年/月/姓名/部门筛选，支持导出 Excel 出勤表
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/timesheet")
public class AdminTimesheetController {

    private final TimesheetRecordMapper mapper;
    private final SystemConfigService configService;

    public AdminTimesheetController(TimesheetRecordMapper mapper, SystemConfigService configService) {
        this.mapper = mapper;
        this.configService = configService;
    }

    /**
     * 查询指定月份所有员工的工时记录
     * GET /api/admin/timesheet/query?year=2026&month=8&name=张三&dept=technology
     */
    @GetMapping("/query")
    public ApiResponse<List<Map<String, Object>>> query(
            @RequestParam int year,
            @RequestParam int month,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String dept) {
        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());

        List<Map<String, Object>> records = mapper.findAllByMonthWithEmployee(
                startDate, endDate,
                (name != null && !name.isBlank()) ? name.trim() : null,
                (dept != null && !dept.isBlank()) ? dept.trim() : null
        );

        log.info("历史查询: {}-{} name={} dept={} => {} 条记录", year, month, name, dept, records.size());
        return ApiResponse.success(records);
    }

    /**
     * 导出指定月份出勤 Excel（截图样式）
     * GET /api/admin/timesheet/export?year=2026&month=8&name=张三&dept=technology
     * <p>
     * 布局：
     * 第 1 行：大标题“上海融永机械设备制造有限公司    如皋车间出勤    XXXX年X月”（合并居中）
     * 第 3 行：表头“序号、姓名、1、2...31、合计”
     * 数据行：工作日显示 ✓ 与 +加班时长（上下两行），休息日显示“休”，合计列自动汇总加班总时长
     */
    @GetMapping("/export")
    public void export(@RequestParam int year,
                       @RequestParam int month,
                       @RequestParam(required = false) String name,
                       @RequestParam(required = false) String dept,
                       HttpServletResponse response) throws IOException {
        LocalDate startDate = LocalDate.of(year, month, 1);
        int daysInMonth = startDate.lengthOfMonth();
        LocalDate endDate = startDate.withDayOfMonth(daysInMonth);

        List<Map<String, Object>> records = mapper.findAllByMonthWithEmployee(
                startDate, endDate,
                (name != null && !name.isBlank()) ? name.trim() : null,
                (dept != null && !dept.isBlank()) ? dept.trim() : null
        );

        // 按 员工姓名 -> 日期 组织数据
        Map<String, Map<LocalDate, Map<String, Object>>> empMap = new TreeMap<>();
        for (Map<String, Object> r : records) {
            String empName = (String) r.get("employee_name");
            LocalDate d = ((java.sql.Date) r.get("date")).toLocalDate();
            empMap.computeIfAbsent(empName, k -> new TreeMap<>()).put(d, r);
        }

        List<String> empNames = new ArrayList<>(empMap.keySet());

        // 读取节假日/调班配置
        List<HolidayItem> holidays = configService.getHolidays();

        // 总列数：序号 + 姓名 + 日期 + 合计
        int totalCols = 2 + daysInMonth + 1;

        // 创建 Workbook
        XSSFWorkbook wb = new XSSFWorkbook();
        XSSFSheet sheet = wb.createSheet(year + "年" + month + "月");

        // 设置列宽
        sheet.setColumnWidth(0, 1800); // 序号
        sheet.setColumnWidth(1, 3200); // 姓名
        for (int d = 1; d <= daysInMonth; d++) {
            sheet.setColumnWidth(1 + d, 1300);
        }
        sheet.setColumnWidth(totalCols - 1, 2200); // 合计

        // 通用居中样式
        CellStyle centerStyle = wb.createCellStyle();
        centerStyle.setAlignment(HorizontalAlignment.CENTER);
        centerStyle.setVerticalAlignment(VerticalAlignment.CENTER);

        // 标题行：第 1 行，合并所有列
        Row titleRow = sheet.createRow(0);
        titleRow.setHeightInPoints(38);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("上海融永机械设备制造有限公司    如皋车间出勤    " + year + "年" + month + "月");
        XSSFFont titleFont = wb.createFont();
        titleFont.setFontName("微软雅黑");
        titleFont.setFontHeightInPoints((short) 16);
        titleFont.setBold(true);
        CellStyle titleStyle = wb.createCellStyle();
        titleStyle.setFont(titleFont);
        titleStyle.setAlignment(HorizontalAlignment.CENTER);
        titleStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        titleCell.setCellStyle(titleStyle);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, totalCols - 1));

        // 空行（第 2 行）
        sheet.createRow(1).setHeightInPoints(8);

        // 表头行（第 3 行）
        Row headerRow = sheet.createRow(2);
        headerRow.setHeightInPoints(26);
        XSSFFont headerFont = wb.createFont();
        headerFont.setFontName("微软雅黑");
        headerFont.setFontHeightInPoints((short) 11);
        headerFont.setBold(true);
        CellStyle headerStyle = wb.createCellStyle();
        headerStyle.setFont(headerFont);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);
        headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        ((XSSFCellStyle) headerStyle).setFillForegroundColor(new XSSFColor(new byte[]{(byte) 230, (byte) 230, (byte) 230}, null));
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setBorderBottom(BorderStyle.THIN);
        headerStyle.setBorderTop(BorderStyle.THIN);
        headerStyle.setBorderLeft(BorderStyle.THIN);
        headerStyle.setBorderRight(BorderStyle.THIN);

        String[] headers = new String[totalCols];
        headers[0] = "序号";
        headers[1] = "姓名";
        for (int d = 1; d <= daysInMonth; d++) {
            headers[1 + d] = String.valueOf(d);
        }
        headers[totalCols - 1] = "合计";

        for (int c = 0; c < totalCols; c++) {
            Cell cell = headerRow.createCell(c);
            cell.setCellValue(headers[c]);
            cell.setCellStyle(headerStyle);
        }

        // 日期单元格样式：休息日浅灰
        CellStyle restStyle = wb.createCellStyle();
        restStyle.cloneStyleFrom(centerStyle);
        ((XSSFCellStyle) restStyle).setFillForegroundColor(new XSSFColor(new byte[]{(byte) 240, (byte) 240, (byte) 240}, null));
        restStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        restStyle.setBorderBottom(BorderStyle.THIN);
        restStyle.setBorderTop(BorderStyle.THIN);
        restStyle.setBorderLeft(BorderStyle.THIN);
        restStyle.setBorderRight(BorderStyle.THIN);

        // 日期单元格样式：出勤日
        CellStyle workStyle = wb.createCellStyle();
        workStyle.cloneStyleFrom(centerStyle);
        workStyle.setWrapText(true);
        workStyle.setBorderBottom(BorderStyle.THIN);
        workStyle.setBorderTop(BorderStyle.THIN);
        workStyle.setBorderLeft(BorderStyle.THIN);
        workStyle.setBorderRight(BorderStyle.THIN);

        // 数据行样式
        CellStyle nameStyle = wb.createCellStyle();
        nameStyle.cloneStyleFrom(centerStyle);
        nameStyle.setBorderBottom(BorderStyle.THIN);
        nameStyle.setBorderTop(BorderStyle.THIN);
        nameStyle.setBorderLeft(BorderStyle.THIN);
        nameStyle.setBorderRight(BorderStyle.THIN);

        CellStyle totalStyle = wb.createCellStyle();
        totalStyle.cloneStyleFrom(centerStyle);
        totalStyle.setBorderBottom(BorderStyle.THIN);
        totalStyle.setBorderTop(BorderStyle.THIN);
        totalStyle.setBorderLeft(BorderStyle.THIN);
        totalStyle.setBorderRight(BorderStyle.THIN);
        ((XSSFCellStyle) totalStyle).setFillForegroundColor(new XSSFColor(new byte[]{(byte) 255, (byte) 249, (byte) 235}, null));
        totalStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        XSSFFont checkFont = wb.createFont();
        checkFont.setFontName("Segoe UI Symbol");
        checkFont.setFontHeightInPoints((short) 14);
        checkFont.setColor(IndexedColors.GREEN.getIndex());

        XSSFFont plusFont = wb.createFont();
        plusFont.setFontName("微软雅黑");
        plusFont.setFontHeightInPoints((short) 9);
        plusFont.setColor(IndexedColors.GREEN.getIndex());

        XSSFFont restFont = wb.createFont();
        restFont.setFontName("微软雅黑");
        restFont.setFontHeightInPoints((short) 11);
        restFont.setColor(IndexedColors.GREY_50_PERCENT.getIndex());

        // 填充数据行
        int rowIdx = 3;
        for (int i = 0; i < empNames.size(); i++) {
            String empName = empNames.get(i);
            Map<LocalDate, Map<String, Object>> dateMap = empMap.get(empName);
            Row row = sheet.createRow(rowIdx++);
            row.setHeightInPoints(32);

            // 序号
            Cell noCell = row.createCell(0);
            noCell.setCellValue(i + 1);
            noCell.setCellStyle(nameStyle);

            // 姓名
            Cell nameCell = row.createCell(1);
            nameCell.setCellValue(empName);
            nameCell.setCellStyle(nameStyle);

            double totalOvertime = 0.0;

            // 日期列
            for (int d = 1; d <= daysInMonth; d++) {
                LocalDate date = LocalDate.of(year, month, d);
                int col = 1 + d;
                Cell cell = row.createCell(col);

                if (isRestDay(date, holidays)) {
                    cell.setCellValue("休");
                    CellStyle rs = wb.createCellStyle();
                    rs.cloneStyleFrom(restStyle);
                    rs.setFont(restFont);
                    cell.setCellStyle(rs);
                } else {
                    Map<String, Object> rec = dateMap.get(date);
                    if (rec != null) {
                        BigDecimal oh = (BigDecimal) rec.get("overtime_hours");
                        double ohVal = oh != null ? oh.doubleValue() : 0.0;
                        totalOvertime += ohVal;

                        if (ohVal > 0) {
                            String ohText = stripTrailingZero(ohVal);
                            XSSFRichTextString rich = new XSSFRichTextString("✓\n+" + ohText);
                            rich.applyFont(0, 1, checkFont);
                            rich.applyFont(2, rich.length(), plusFont);
                            cell.setCellValue(rich);
                        } else {
                            cell.setCellValue("✓");
                            CellStyle ws = wb.createCellStyle();
                            ws.cloneStyleFrom(workStyle);
                            ws.setFont(checkFont);
                            cell.setCellStyle(ws);
                            continue;
                        }
                    }
                    cell.setCellStyle(workStyle);
                }
            }

            // 合计列
            Cell totalCell = row.createCell(totalCols - 1);
            totalCell.setCellValue(stripTrailingZero(totalOvertime));
            totalCell.setCellStyle(totalStyle);
        }

        // 响应头
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String fileName = URLEncoder.encode(
                year + "年" + month + "月出勤表",
                StandardCharsets.UTF_8
        );
        response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");

        // 写出
        try (var out = response.getOutputStream()) {
            wb.write(out);
        }
        wb.close();

        log.info("历史查询导出: {}-{} name={} dept={} 共 {} 人", year, month, name, dept, empNames.size());
    }

    /**
     * 判断某天是否为休息日
     * 优先级：调班日（上班） > 法定节假日（休息） > 周末（休息）
     */
    private boolean isRestDay(LocalDate date, List<HolidayItem> holidays) {
        DateTimeFormatter fmt = DateTimeFormatter.ISO_LOCAL_DATE;
        String dateStr = date.format(fmt);

        for (HolidayItem item : holidays) {
            if (item == null || item.getStartDate() == null || item.getEndDate() == null) continue;
            LocalDate start = LocalDate.parse(item.getStartDate(), fmt);
            LocalDate end = LocalDate.parse(item.getEndDate(), fmt);
            if (!date.isBefore(start) && !date.isAfter(end)) {
                return !"shift".equals(item.getType()); // shift=调班日（上班），其他 holiday=休息
            }
        }

        // 未命中配置，按周末休息处理
        return date.getDayOfWeek().getValue() >= 6;
    }

    /**
     * 去掉小数末尾的 0，如 2.0 -> "2"，2.5 -> "2.5"
     */
    private String stripTrailingZero(double value) {
        if (value == (long) value) {
            return String.valueOf((long) value);
        }
        return BigDecimal.valueOf(value).stripTrailingZeros().toPlainString();
    }
}
