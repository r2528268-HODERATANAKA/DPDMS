package com.dpdms.discovery_server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

// The Eureka "phonebook" of the DPDMS system.
// Every service registers itself here at startup, and the api-gateway uses
// this registry to find the live address of each service (lb://... URIs).
@SpringBootApplication
@EnableEurekaServer
public class DiscoveryServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(DiscoveryServerApplication.class, args);
	}

}
