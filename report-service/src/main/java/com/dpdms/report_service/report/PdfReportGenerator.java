package com.dpdms.report_service.report;

import com.lowagie.text.Document;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

// PDF engine (OpenPDF). Title paragraph + one table with all incidents.
@Component
public class PdfReportGenerator implements ReportGenerator {

    @Override
    public String format() {
        return "pdf";
    }

    @Override
    public String extension() {
        return "pdf";
    }

    @Override
    public String contentType() {
        return "application/pdf";
    }

    @Override
    public byte[] build(List<Map<String, Object>> rows, String title) {
        try (var out = new java.io.ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4.rotate());
            PdfWriter.getInstance(document, out);
            document.open();

            Font titleFont = new Font(Font.HELVETICA, 16, Font.BOLD, new Color(20, 45, 90));
            document.add(new Paragraph(title, titleFont));
            document.add(new Paragraph("Generated: " + java.time.LocalDateTime.now()));
            document.add(new Paragraph(" "));

            Set<String> columns = columnsOf(rows);
            PdfPTable table = new PdfPTable(columns.size());
            table.setWidthPercentage(100);
            Font headerFont = new Font(Font.HELVETICA, 9, Font.BOLD);
            for (String column : columns) {
                table.addCell(new Paragraph(column, headerFont));
            }
            for (Map<String, Object> row : rows) {
                for (String column : columns) {
                    Object value = row.get(column);
                    table.addCell(value == null ? "" : value.toString());
                }
            }
            document.add(table);
            document.close();
            return out.toByteArray();
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to build PDF report: " + ex.getMessage(), ex);
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
