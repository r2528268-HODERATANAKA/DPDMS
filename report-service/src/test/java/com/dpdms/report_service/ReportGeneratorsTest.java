package com.dpdms.report_service;

import com.dpdms.report_service.report.CsvReportGenerator;
import com.dpdms.report_service.report.ExcelReportGenerator;
import com.dpdms.report_service.report.PdfReportGenerator;
import com.dpdms.report_service.report.WordReportGenerator;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

// Each generator must produce a real, valid file of its kind.
class ReportGeneratorsTest {

    private final List<Map<String, Object>> rows = List.of(
            Map.of("id", 1, "ward", "Mudzi", "district", "Mudzi",
                    "province", "Mashonaland Central", "severity", "HIGH",
                    "status", "APPROVED", "areaBurnedHectares", 45.5),
            Map.of("id", 2, "ward", "Kanyemba", "district", "Mbire",
                    "province", "Mashonaland Central", "severity", "MEDIUM",
                    "status", "APPROVED", "areaBurnedHectares", 12.0));

    private final String title = "DPDMS FIRE incident report";

    @Test
    void csvReportHasTitleHeaderAndDataRows() {
        byte[] bytes = new CsvReportGenerator().build(rows, title);
        String csv = new String(bytes, StandardCharsets.UTF_8);

        String[] lines = csv.split("\n");
        assertTrue(lines[0].startsWith("DPDMS FIRE"));       // title line
        assertTrue(lines[1].contains("ward"));               // header row
        assertTrue(csv.contains("Mudzi"));                   // data
        assertTrue(csv.contains("45.5"));                    // hazard-specific column
        assertEquals(4, lines.length);                       // title + header + 2 rows
    }

    @Test
    void pdfReportStartsWithPdfMagicBytes() {
        byte[] bytes = new PdfReportGenerator().build(rows, title);
        assertTrue(bytes.length > 100);
        assertEquals("%PDF", new String(bytes, 0, 4, StandardCharsets.US_ASCII));
    }

    @Test
    void excelReportIsAZipContainer() {
        byte[] bytes = new ExcelReportGenerator().build(rows, title);
        assertTrue(bytes.length > 100);
        // .xlsx is a zip archive -> starts with the "PK" magic bytes
        assertEquals('P', bytes[0]);
        assertEquals('K', bytes[1]);
    }

    @Test
    void wordReportIsAZipContainer() {
        byte[] bytes = new WordReportGenerator().build(rows, title);
        assertTrue(bytes.length > 100);
        assertEquals('P', bytes[0]);
        assertEquals('K', bytes[1]);
    }
}
