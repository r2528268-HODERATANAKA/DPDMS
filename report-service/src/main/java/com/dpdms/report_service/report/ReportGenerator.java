package com.dpdms.report_service.report;

import java.util.List;
import java.util.Map;

// One report engine. This is the STRATEGY pattern: ReportService only knows
// this interface; the four implementations (CSV, PDF, Excel, Word) are picked
// by the "format" query parameter. Adding a new format later = one new class.
public interface ReportGenerator {

    /** Value accepted in the ?format= parameter, e.g. "pdf". */
    String format();

    /** File extension for the download, e.g. "pdf". */
    String extension();

    /** Content-Type header for the HTTP response. */
    String contentType();

    /** Build the whole report file in memory. */
    byte[] build(List<Map<String, Object>> rows, String title);
}
