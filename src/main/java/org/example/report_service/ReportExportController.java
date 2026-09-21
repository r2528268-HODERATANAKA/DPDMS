package org.example.report_service;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/reports/export")
public class ReportExportController {

    private final ReportService reportService;
    private final CsvReportExporter csvReportExporter;
    private final XlsxReportExporter xlsxReportExporter;
    private final PdfReportExporter pdfReportExporter;
    private final DocxReportExporter docxReportExporter;

    public ReportExportController(
            ReportService reportService,
            CsvReportExporter csvReportExporter,
            XlsxReportExporter xlsxReportExporter,
            PdfReportExporter pdfReportExporter,
            DocxReportExporter docxReportExporter) {

        this.reportService = reportService;
        this.csvReportExporter = csvReportExporter;
        this.xlsxReportExporter = xlsxReportExporter;
        this.pdfReportExporter = pdfReportExporter;
        this.docxReportExporter = docxReportExporter;
    }

    @GetMapping("/csv")
    public ResponseEntity<byte[]> exportCsv(
            @RequestParam(required = false) String hazard,
            @RequestParam(required = false) String ward,
            @RequestParam(required = false) String district,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String severity,
            @RequestParam(required = false) String status) {

        List<ReportRecord> records = reportService.filterRecords(
                hazard, ward, district, startDate, endDate, severity, status);

        String csv = csvReportExporter.export(records);

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=dpdms-zoonotic-report.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv.getBytes());
    }

    @GetMapping("/xlsx")
    public ResponseEntity<byte[]> exportXlsx(
            @RequestParam(required = false) String hazard,
            @RequestParam(required = false) String ward,
            @RequestParam(required = false) String district,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String severity,
            @RequestParam(required = false) String status) {

        try {
            List<ReportRecord> records = reportService.filterRecords(
                    hazard, ward, district, startDate, endDate, severity, status);

            byte[] file = xlsxReportExporter.export(records);

            return ResponseEntity.ok()
                    .header(
                            HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=dpdms-zoonotic-report.xlsx")
                    .contentType(MediaType.parseMediaType(
                            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(file);

        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/pdf")
    public ResponseEntity<byte[]> exportPdf(
            @RequestParam(required = false) String hazard,
            @RequestParam(required = false) String ward,
            @RequestParam(required = false) String district,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String severity,
            @RequestParam(required = false) String status) {

        try {
            List<ReportRecord> records = reportService.filterRecords(
                    hazard, ward, district, startDate, endDate, severity, status);

            byte[] file = pdfReportExporter.export(records);

            return ResponseEntity.ok()
                    .header(
                            HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=dpdms-zoonotic-report.pdf")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(file);

        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/docx")
    public ResponseEntity<byte[]> exportDocx(
            @RequestParam(required = false) String hazard,
            @RequestParam(required = false) String ward,
            @RequestParam(required = false) String district,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String severity,
            @RequestParam(required = false) String status) {

        try {
            List<ReportRecord> records = reportService.filterRecords(
                    hazard, ward, district, startDate, endDate, severity, status);

            byte[] file = docxReportExporter.export(records);

            return ResponseEntity.ok()
                    .header(
                            HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=dpdms-zoonotic-report.docx")
                    .contentType(MediaType.parseMediaType(
                            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
                    .body(file);

        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}