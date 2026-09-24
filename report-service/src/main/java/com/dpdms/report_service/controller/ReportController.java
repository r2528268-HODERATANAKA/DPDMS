package com.dpdms.report_service.controller;

import com.dpdms.report_service.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

// Report endpoints (all GET, all return a downloadable file):
//   GET /api/reports/flood?format=pdf
//   GET /api/reports/drought?format=xlsx
//   GET /api/reports/fire?format=docx
//   GET /api/reports/zoonotic?format=csv
//   GET /api/reports/mining?format=pdf     (format is optional - default pdf)
// The caller's X-User-* headers are forwarded so ward scoping still applies.
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService service;

    @GetMapping("/{hazard}")
    public ResponseEntity<byte[]> report(
            @PathVariable String hazard,
            @RequestParam(defaultValue = "pdf") String format,
            @RequestHeader(value = "X-User-Name", required = false) String callerName,
            @RequestHeader(value = "X-User-Role", required = false) String callerRole,
            @RequestHeader(value = "X-User-Ward", required = false) String callerWard,
            @RequestHeader(value = "X-User-Hazard", required = false) String callerHazard) {

        HttpHeaders identity = new HttpHeaders();
        setIfPresent(identity, "X-User-Name", callerName);
        setIfPresent(identity, "X-User-Role", callerRole);
        setIfPresent(identity, "X-User-Ward", callerWard);
        setIfPresent(identity, "X-User-Hazard", callerHazard);

        var generator = service.generatorFor(format);
        byte[] file = service.buildReport(hazard, format, identity);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(generator.contentType()))
                .header("Content-Disposition",
                        "attachment; filename=" + hazard + "-report-"
                                + LocalDate.now() + "." + generator.extension())
                .body(file);
    }

    private void setIfPresent(HttpHeaders headers, String name, String value) {
        if (value != null) {
            headers.set(name, value);
        }
    }
}
