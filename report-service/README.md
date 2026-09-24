# report-service

Turns APPROVED incidents into downloadable **CSV / PDF / XLSX / DOCX** reports.

**Owner:** Rejoice   |   **Port:** 8086   |   **No database** (reports live data)

## Files in this service (what each one does)

| File | What it does |
|------|--------------|
| `pom.xml` | Boot 4.1.1 web starter + **Apache POI 5.3.0** (Excel + Word) + **OpenPDF 1.3.43** (PDF). CSV needs no library. |
| `ReportServiceApplication.java` | Entry point. |
| `report/ReportGenerator.java` | **Strategy interface**: `format()`, `extension()`, `contentType()`, `build(rows, title)`. |
| `report/CsvReportGenerator.java` | Pure-Java CSV: title line, header = union of all columns (stable order), quoted escaping. |
| `report/PdfReportGenerator.java` | OpenPDF: A4 landscape, title + table. |
| `report/ExcelReportGenerator.java` | POI XSSF `.xlsx`: title row, bold header, numbers typed as numbers. |
| `report/WordReportGenerator.java` | POI XWPF `.docx`: centered title + table. |
| `client/HazardDataClient.java` | Fetches the approved feed `GET /api/<plural>` of the chosen hazard, **forwarding the caller's X-User-\* headers** so ward scoping still applies. Unknown hazard → clear 400. Dead service → 502 with a readable message. |
| `service/ReportService.java` | Picks the right generator for the requested format (no if-chains — stream over the strategies) and returns the file bytes. |
| `controller/ReportController.java` | `GET /api/reports/{hazard}?format=csv|pdf|xlsx|docx` — returns the bytes with `Content-Disposition: attachment; filename=hazard-report-DATE.ext`. |
| `exception/GlobalExceptionHandler.java` | Unknown hazard/format → 400, upstream failure → 502. |
| `ReportGeneratorsTest.java` (4 tests) | CSV structure (title/header/rows), PDF starts with `%PDF`, XLSX and DOCX are valid zip containers. |
| `ReportServiceTest.java` (3 tests) | CSV built from fetched rows, unknown format rejected with a helpful message, unknown hazard rejected. |

## Try it

```bash
curl -OJ "http://localhost:8086/api/reports/fires?format=pdf"      # direct
curl -OJ "http://localhost:8888/api/reports/fires?format=xlsx" \
     -H "Authorization: Bearer $TOKEN"                             # through the gateway
```

## Why no database?

A report is a *view* of live data. Fetching at report time means the report can
never be stale and there is nothing to synchronise. The trade-off (report
service is down when hazard services are down) is deliberate and worth saying
out loud in the demo.

## If the teacher asks

- **Design pattern?** Strategy — four `ReportGenerator` implementations; the
  service selects by `format` string. Adding XML tomorrow = one class.
- **How does ward scoping survive the hop?** The caller's `X-User-*` headers are
  forwarded verbatim; the hazard service enforces them exactly as if the caller
  had come directly.
- **Which library for which format?** CSV: none (standard library). XLSX + DOCX:
  Apache POI. PDF: OpenPDF (LGPL fork of iText 4).
