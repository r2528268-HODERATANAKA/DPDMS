package org.example.report_service;

import com.lowagie.text.Document;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.util.List;

@Component
public class PdfReportExporter {

    public byte[] export(List<ReportRecord> records) {

        try {
            ByteArrayOutputStream outputStream =
                    new ByteArrayOutputStream();

            Document document = new Document();

            PdfWriter.getInstance(document, outputStream);

            document.open();

            document.add(
                    new Paragraph("DPDMS Zoonotic Disease Report")
            );

            document.add(
                    new Paragraph("Approved incidents only")
            );

            document.add(new Paragraph(" "));

            PdfPTable table = new PdfPTable(8);

            table.addCell(new Phrase("ID"));
            table.addCell(new Phrase("Hazard"));
            table.addCell(new Phrase("Ward"));
            table.addCell(new Phrase("District"));
            table.addCell(new Phrase("Date"));
            table.addCell(new Phrase("Reporter"));
            table.addCell(new Phrase("Severity"));
            table.addCell(new Phrase("Status"));

            for (ReportRecord record : records) {

                table.addCell(value(record.getId()));
                table.addCell(value(record.getHazard()));
                table.addCell(value(record.getWard()));
                table.addCell(value(record.getDistrict()));
                table.addCell(value(record.getIncidentDateTime()));
                table.addCell(value(record.getReporter()));
                table.addCell(value(record.getSeverity()));
                table.addCell(value(record.getStatus()));
            }

            document.add(table);

            document.close();

            return outputStream.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to generate PDF report",
                    e
            );
        }
    }

    private String value(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}