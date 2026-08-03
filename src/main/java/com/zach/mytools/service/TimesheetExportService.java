package com.zach.mytools.service;

import com.zach.mytools.dto.HolidayItem;
import com.zach.mytools.entity.Employee;
import com.zach.mytools.entity.TimesheetRecord;
import com.zach.mytools.mapper.TimesheetRecordMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.*;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 工时导出服务（Apache POI 出勤表格式）
 * <p>
 * 与 AdminTimesheetController 使用相同的出勤表格式：
 * 标题行 → 表头（序号/姓名/1~31/合计）→ 数据行（✓ + 加班时长 / 休）
 */
@Slf4j
@Service
public class TimesheetExportService {

    private final TimesheetRecordMapper mapper;
    private final SystemConfigService configService;

    public TimesheetExportService(TimesheetRecordMapper mapper, SystemConfigService configService) {
        this.mapper = mapper;
        this.configService = configService;
    }

    /**
     * 导出当前员工指定月份的出勤表 Excel
     */
    public void export(HttpServletResponse response, Employee employee, int year, int month) throws IOException {
        LocalDate startDate = LocalDate.of(year, month, 1);
        int daysInMonth = startDate.lengthOfMonth();
        LocalDate endDate = startDate.withDayOfMonth(daysInMonth);

        // 查询当月该员工的工时记录
        List<TimesheetRecord> records = mapper.findByEmpIdAndDateRange(employee.getId(), startDate, endDate);
        Map<LocalDate, TimesheetRecord> recordMap = new HashMap<>();
        for (TimesheetRecord r : records) {
            recordMap.put(r.getDate(), r);
        }

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

        // ===== 标题行（第 1 行）=====
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

        // ===== 表头行（第 3 行）=====
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
        ((XSSFCellStyle) headerStyle).setFillForegroundColor(
                new XSSFColor(new byte[]{(byte) 230, (byte) 230, (byte) 230}, null));
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setBorderBottom(BorderStyle.THIN);
        headerStyle.setBorderTop(BorderStyle.THIN);
        headerStyle.setBorderLeft(BorderStyle.THIN);
        headerStyle.setBorderRight(BorderStyle.THIN);

        Cell h0 = headerRow.createCell(0); h0.setCellValue("序号"); h0.setCellStyle(headerStyle);
        Cell h1 = headerRow.createCell(1); h1.setCellValue("姓名"); h1.setCellStyle(headerStyle);
        for (int d = 1; d <= daysInMonth; d++) {
            Cell c = headerRow.createCell(1 + d);
            c.setCellValue(String.valueOf(d));
            c.setCellStyle(headerStyle);
        }
        Cell hTotal = headerRow.createCell(totalCols - 1);
        hTotal.setCellValue("合计");
        hTotal.setCellStyle(headerStyle);

        // ===== 预定义样式 =====
        // 休息日样式
        CellStyle restStyle = wb.createCellStyle();
        restStyle.cloneStyleFrom(centerStyle);
        ((XSSFCellStyle) restStyle).setFillForegroundColor(
                new XSSFColor(new byte[]{(byte) 240, (byte) 240, (byte) 240}, null));
        restStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        restStyle.setBorderBottom(BorderStyle.THIN);
        restStyle.setBorderTop(BorderStyle.THIN);
        restStyle.setBorderLeft(BorderStyle.THIN);
        restStyle.setBorderRight(BorderStyle.THIN);

        // 工作日样式
        CellStyle workStyle = wb.createCellStyle();
        workStyle.cloneStyleFrom(centerStyle);
        workStyle.setWrapText(true);
        workStyle.setBorderBottom(BorderStyle.THIN);
        workStyle.setBorderTop(BorderStyle.THIN);
        workStyle.setBorderLeft(BorderStyle.THIN);
        workStyle.setBorderRight(BorderStyle.THIN);

        // 序号/姓名样式
        CellStyle nameStyle = wb.createCellStyle();
        nameStyle.cloneStyleFrom(centerStyle);
        nameStyle.setBorderBottom(BorderStyle.THIN);
        nameStyle.setBorderTop(BorderStyle.THIN);
        nameStyle.setBorderLeft(BorderStyle.THIN);
        nameStyle.setBorderRight(BorderStyle.THIN);

        // 合计列样式（浅橙底色）
        CellStyle totalStyle = wb.createCellStyle();
        totalStyle.cloneStyleFrom(centerStyle);
        totalStyle.setBorderBottom(BorderStyle.THIN);
        totalStyle.setBorderTop(BorderStyle.THIN);
        totalStyle.setBorderLeft(BorderStyle.THIN);
        totalStyle.setBorderRight(BorderStyle.THIN);
        ((XSSFCellStyle) totalStyle).setFillForegroundColor(
                new XSSFColor(new byte[]{(byte) 255, (byte) 249, (byte) 235}, null));
        totalStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        // 字体
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

        // ===== 数据行（第 4 行）=====
        Row row = sheet.createRow(3);
        row.setHeightInPoints(32);

        // 序号
        Cell noCell = row.createCell(0);
        noCell.setCellValue(1);
        noCell.setCellStyle(nameStyle);

        // 姓名
        Cell nameCell = row.createCell(1);
        nameCell.setCellValue(employee.getName());
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
                TimesheetRecord rec = recordMap.get(date);
                if (rec != null) {
                    BigDecimal oh = rec.getOvertimeHours();
                    double ohVal = (oh != null) ? oh.doubleValue() : 0.0;
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

        // ===== 响应头 =====
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String fileName = URLEncoder.encode(
                employee.getName() + "_" + year + "年" + month + "月出勤表",
                StandardCharsets.UTF_8
        );
        response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");

        // 写出
        try (var out = response.getOutputStream()) {
            wb.write(out);
        }
        wb.close();

        log.info("导出出勤表: {} {}-{}  记录数: {}", employee.getName(), year, month, records.size());
    }

    /**
     * 判断某天是否为休息日
     * 优先级：调班日（上班） > 法���节假日（休息） > 周末（休息）
     */
    private boolean isRestDay(LocalDate date, List<HolidayItem> holidays) {
        DateTimeFormatter fmt = DateTimeFormatter.ISO_LOCAL_DATE;
        String dateStr = date.format(fmt);

        for (HolidayItem item : holidays) {
            if (item == null || item.getStartDate() == null || item.getEndDate() == null) continue;
            LocalDate start = LocalDate.parse(item.getStartDate(), fmt);
            LocalDate end = LocalDate.parse(item.getEndDate(), fmt);
            if (!date.isBefore(start) && !date.isAfter(end)) {
                return !"shift".equals(item.getType());
            }
        }

        return date.getDayOfWeek().getValue() >= 6;
    }

    /**
     * 去掉小数末尾的 0，如 2.0 → "2"，2.5 → "2.5"
     */
    private String stripTrailingZero(double value) {
        if (value == (long) value) {
            return String.valueOf((long) value);
        }
        return BigDecimal.valueOf(value).stripTrailingZeros().toPlainString();
    }
}
