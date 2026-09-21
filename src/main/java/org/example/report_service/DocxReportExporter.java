package org.example.report_service;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

@Component
public class DocxReportExporter {

    public byte[] export(List<ReportRecord> records) throws IOException {

        try (XWPFDocument document = new XWPFDocument();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            document.createParagraph()
                    .createRun()
                    .setText("DPDMS Zoonotic Disease Report");

            document.createParagraph()
                    .createRun()
                    .setText("Approved incidents only");

            XWPFTable table = document.createTable(1, 8);

            XWPFTableRow header = table.getRow(0);

            header.getCell(0).setText("ID");
            header.getCell(1).setText("Hazard");
            header.getCell(2).setText("Ward");
            header.getCell(3).setText("District");
            header.getCell(4).setText("Date");
            header.getCell(5).setText("Reporter");
            header.getCell(6).setText("Severity");
            header.getCell(7).setText("Status");

            for (ReportRecord record : records) {

                XWPFTableRow row = table.createRow();

                row.getCell(0).setText(value(record.getId()));
                row.getCell(1).setText(value(record.getHazard()));
                row.getCell(2).setText(value(record.getWard()));
                row.getCell(3).setText(value(record.getDistrict()));
                row.getCell(4).setText(value(record.getIncidentDateTime()));
                row.getCell(5).setText(value(record.getReporter()));
                row.getCell(6).setText(value(record.getSeverity()));
                row.getCell(7).setText(value(record.getStatus()));
            }

            document.write(outputStream);

            return outputStream.toByteArray();
        }
    }

    private String value(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
