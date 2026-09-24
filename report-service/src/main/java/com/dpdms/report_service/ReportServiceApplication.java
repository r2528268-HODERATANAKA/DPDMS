package com.dpdms.report_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// DPDMS report-service: turns APPROVED incidents from the five hazard services
// into downloadable PDF / DOCX / XLSX / CSV reports.
// NOTE: this service has no database of its own - it always reports live data.
@SpringBootApplication
public class ReportServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(ReportServiceApplication.class, args);
	}

}
