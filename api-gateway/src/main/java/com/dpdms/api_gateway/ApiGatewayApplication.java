package com.dpdms.api_gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// The single entry point of the DPDMS backend (port 8888).
// Clients talk to THIS service only; it verifies their JWT and forwards the
// request to the right microservice (found via Eureka).
@SpringBootApplication
public class ApiGatewayApplication {

	public static void main(String[] args) {
		SpringApplication.run(ApiGatewayApplication.class, args);
	}

}
