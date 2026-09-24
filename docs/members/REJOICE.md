# Rejoice's part — zoonotic-disease-service + report-service

You own a hazard slice and the reporting engine that turns approved data into
the files the province actually sends around.

## zoonotic-disease-service (port 8084, db dpdms_zoonotic)

An exact twin of flood-service. Your fields: `diseaseName`,
`suspectedAnimalSpecies`, `humanCases`, `humanDeaths`, `animalsAffected`,
`outbreakStatus` (SUSPECTED/CONFIRMED/UNDER_CONTROL).

| File | One-sentence explanation |
|------|--------------------------|
| `model/ZoonoticIncident.java` | Row in `zoonotic_incidents`: shared metadata + GPS + your outbreak indicators + audit trail. |
| `model/OutbreakStatus.java` | SUSPECTED → CONFIRMED → UNDER_CONTROL. |
| `service/ZoonoticIncidentService.java` | Same rules as every hazard service (PENDING on create, 403 scoping, approved locked, audited reviews). |
| `service/AlertNotifier.java` | On approve → best-effort POST to alert-service. |
| `controller/ZoonoticIncidentController.java` | 9 endpoints under `/api/zoonotics`. |
| `ZoonoticIncidentServiceTest.java` | 13 unit tests, one per rule. |

## report-service (port 8086, deliberately no database)

| File | One-sentence explanation |
|------|--------------------------|
| `report/ReportGenerator.java` | Strategy interface: format/extension/contentType/build — the polymorphism of your part. |
| `report/CsvReportGenerator.java` | Pure Java CSV: title line, union-of-columns header, quoted escaping. |
| `report/PdfReportGenerator.java` | OpenPDF: A4 landscape title + table. |
| `report/ExcelReportGenerator.java` | Apache POI XSSF: bold header, numeric cells typed as numbers. |
| `report/WordReportGenerator.java` | Apache POI XWPF: centered title + table. |
| `client/HazardDataClient.java` | Fetches the chosen hazard's approved feed, forwarding the caller's X-User-* headers (ward scoping survives the hop); unknown hazard → 400, dead service → 502. |
| `service/ReportService.java` | Streams over the four generators and picks by `format` — no if-chains. |
| `controller/ReportController.java` | `GET /api/reports/{hazard}?format=csv|pdf|xlsx|docx` with attachment filename. |
| Tests (8) | CSV structure, PDF magic bytes, XLSX/DOCX zip containers, format selection, unknown hazard/format. |

## Your demo (5 minutes)

1. `GET :8888/api/reports/fires?format=pdf` → open the PDF (real file, real data).
2. Same hazard as xlsx → open in Excel — note numbers are numbers, not text.
3. `?format=docx` and `?format=csv` — four formats, one endpoint.
4. Try `?format=xml` → friendly 400 listing the supported formats.
5. Show the code: adding XML = ONE class implementing `ReportGenerator`.

## If the teacher asks YOU

- **Why does report-service have no database?** Reports are a live view; storing
  copies would go stale. Fetch-on-demand means a report is always current.
- **How does ward scoping work when you call other services?** The caller's
  X-User-* headers are forwarded verbatim; the hazard service enforces them as
  if the caller came directly.
- **Which library does what?** CSV: standard library only. XLSX + DOCX: Apache
  POI. PDF: OpenPDF (the maintained LGPL fork of iText 4).
- **Why is zoonotic called zoonotic-disease-service?** The folder/package names
  follow the leader's skeleton (`zoonotic_disease_service`); endpoints are
  `/api/zoonotics`.
