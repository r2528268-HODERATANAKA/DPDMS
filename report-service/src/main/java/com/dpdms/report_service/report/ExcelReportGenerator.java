package com.dpdms.report_service.report;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

// Excel engine (.xlsx via Apache POI). Sheet 1 = title row + header + incidents.
@Component
public class ExcelReportGenerator implements ReportGenerator {

    @Override
    public String format() {
        return "xlsx";
    }

    @Override
    public String extension() {
        return "xlsx";
    }

    @Override
    public String contentType() {
        return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    }

    @Override
    public byte[] build(List<Map<String, Object>> rows, String title) {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             var out = new java.io.ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("DPDMS report");

            Row titleRow = sheet.createRow(0);
            titleRow.createCell(0).setCellValue(title);

            Set<String> columns = columnsOf(rows);
            Row header = sheet.createRow(1);
            CellStyle bold = workbook.createCellStyle();
            Font font = workbook.createFont();
            font.setBold(true);
            bold.setFont(font);
            int c = 0;
            for (String column : columns) {
                Cell cell = header.createCell(c++);
                cell.setCellValue(column);
                cell.setCellStyle(bold);
            }

            int r = 2;
            for (Map<String, Object> row : rows) {
                Row dataRow = sheet.createRow(r++);
                c = 0;
                for (String column : columns) {
                    Object value = row.get(column);
                    Cell cell = dataRow.createCell(c++);
                    if (value instanceof Number number) {
                        cell.setCellValue(number.doubleValue());
                    } else {
                        cell.setCellValue(value == null ? "" : value.toString());
                    }
                }
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to build Excel report: " + ex.getMessage(), ex);
        }
    }

    private Set<String> columnsOf(List<Map<String, Object>> rows) {
        Set<String> columns = new LinkedHashSet<>();
        for (String key : new String[]{"id", "ward", "district", "province", "severity", "status"}) {
            columns.add(key);
        }
        for (Map<String, Object> row : rows) {
            columns.addAll(row.keySet());
        }
        return columns;
    }
}
