package com.dpdms.alert_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// DPDMS alert-service: sends notifications (Email / WhatsApp / Telegram) about
// APPROVED incidents and keeps a log of every attempt.
@SpringBootApplication
public class AlertServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(AlertServiceApplication.class, args);
	}

}
