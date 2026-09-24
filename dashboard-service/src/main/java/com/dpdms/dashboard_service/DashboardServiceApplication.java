package com.dpdms.dashboard_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// DPDMS dashboard-service: ONE call that collects approved incidents from all
// five hazard services - counts by hazard, counts by severity, and the most
// recent approved incidents for the "latest activity" panel.
@SpringBootApplication
public class DashboardServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(DashboardServiceApplication.class, args);
	}

}
