package com.dpdms.fire_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Fire incident microservice - owns the dpdms_fire schema.
 */
@SpringBootApplication
public class FireServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(FireServiceApplication.class, args);
    }
}
