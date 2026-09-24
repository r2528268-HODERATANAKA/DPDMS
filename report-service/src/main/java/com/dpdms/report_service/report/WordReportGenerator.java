package com.dpdms.report_service.report;

import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

// Word engine (.docx via Apache POI XWPF). Title + table of incidents.
@Component
public class WordReportGenerator implements ReportGenerator {

    @Override
    public String format() {
        return "docx";
    }

    @Override
    public String extension() {
        return "docx";
    }

    @Override
    public String contentType() {
        return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
    }

    @Override
    public byte[] build(List<Map<String, Object>> rows, String title) {
        try (XWPFDocument document = new XWPFDocument();
             var out = new java.io.ByteArrayOutputStream()) {
            XWPFParagraph heading = document.createParagraph();
            heading.setAlignment(ParagraphAlignment.CENTER);
            heading.createRun().setText(title);

            Set<String> columns = columnsOf(rows);
            XWPFTable table = document.createTable(rows.size() + 1, columns.size());
            XWPFTableRow headerRow = table.getRow(0);
            int c = 0;
            for (String column : columns) {
                headerRow.getCell(c++).setText(column);
            }
            int r = 1;
            for (Map<String, Object> row : rows) {
                XWPFTableRow dataRow = table.getRow(r++);
                c = 0;
                for (String column : columns) {
                    Object value = row.get(column);
                    dataRow.getCell(c++).setText(value == null ? "" : value.toString());
                }
            }

            document.write(out);
            return out.toByteArray();
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to build Word report: " + ex.getMessage(), ex);
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
