package com.dpdms.report_service.report;

import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

// CSV engine - plain Java, no library. Header row = union of all keys seen in
// the data (stable: common keys first, then hazard-specific ones).
@Component
public class CsvReportGenerator implements ReportGenerator {

    @Override
    public String format() {
        return "csv";
    }

    @Override
    public String extension() {
        return "csv";
    }

    @Override
    public String contentType() {
        return "text/csv";
    }

    @Override
    public byte[] build(List<Map<String, Object>> rows, String title) {
        Set<String> columns = columnsOf(rows);
        StringBuilder sb = new StringBuilder();
        sb.append(escape(title)).append("\n");
        boolean first = true;
        for (String column : columns) {
            if (!first) sb.append(",");
            sb.append(escape(column));
            first = false;
        }
        sb.append("\n");
        for (Map<String, Object> row : rows) {
            first = true;
            for (String column : columns) {
                if (!first) sb.append(",");
                sb.append(escape(row.get(column) == null ? "" : row.get(column).toString()));
                first = false;
            }
            sb.append("\n");
        }
        return sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    private Set<String> columnsOf(List<Map<String, Object>> rows) {
        Set<String> columns = new LinkedHashSet<>();
        // keep the important columns in a friendly order first
        for (String key : new String[]{"id", "ward", "district", "province", "severity", "status"}) {
            columns.add(key);
        }
        for (Map<String, Object> row : rows) {
            columns.addAll(row.keySet());
        }
        return columns;
    }

    private String escape(String value) {
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
