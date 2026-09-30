package com.sonrisa.alerting.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point of the alerting core. Feature packages live below this package
 * (package by feature, architecture Section 6.2).
 */
@SpringBootApplication
public class AlertingApplication {

    public static void main(String[] args) {
        SpringApplication.run(AlertingApplication.class, args);
    }
}
