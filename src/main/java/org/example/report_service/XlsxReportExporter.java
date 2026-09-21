package org.example.report_service;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

@Component
public class XlsxReportExporter {

    public byte[] export(List<ReportRecord> records) throws IOException {

        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("DPDMS Report");

            Row header = sheet.createRow(0);

            String[] columns = {
                    "ID",
                    "Hazard",
                    "Ward",
                    "District",
                    "Province",
                    "Incident Date Time",
                    "Reporter",
                    "Severity",
                    "Status",
                    "Latitude",
                    "Longitude",
                    "Pathogen",
                    "Animal Species",
                    "Human Cases",
                    "Animal Cases",
                    "Cluster/Outbreak Classification"
            };

            for (int i = 0; i < columns.length; i++) {
                header.createCell(i).setCellValue(columns[i]);
            }

            int rowNumber = 1;

            for (ReportRecord record : records) {

                Row row = sheet.createRow(rowNumber++);

                row.createCell(0).setCellValue(value(record.getId()));
                row.createCell(1).setCellValue(value(record.getHazard()));
                row.createCell(2).setCellValue(value(record.getWard()));
                row.createCell(3).setCellValue(value(record.getDistrict()));
                row.createCell(4).setCellValue(value(record.getProvince()));
                row.createCell(5).setCellValue(value(record.getIncidentDateTime()));
                row.createCell(6).setCellValue(value(record.getReporter()));
                row.createCell(7).setCellValue(value(record.getSeverity()));
                row.createCell(8).setCellValue(value(record.getStatus()));
                row.createCell(9).setCellValue(value(record.getLatitude()));
                row.createCell(10).setCellValue(value(record.getLongitude()));
                row.createCell(11).setCellValue(value(record.getPathogenName()));
                row.createCell(12).setCellValue(value(record.getAnimalSpeciesAffected()));
                row.createCell(13).setCellValue(value(record.getConfirmedHumanCases()));
                row.createCell(14).setCellValue(value(record.getConfirmedAnimalCases()));
                row.createCell(15).setCellValue(
                        value(record.getClusterOutbreakClassification())
                );
            }

            for (int i = 0; i < columns.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(outputStream);

            return outputStream.toByteArray();
        }
    }

    private String value(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
