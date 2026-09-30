package com.sonrisa.alerting.spi.archfixture;

import org.springframework.boot.SpringApplication;

/** Fixture: SPI code that depends on Spring Boot (forbidden). */
public class SpiUsingSpringBoot {

    SpringApplication application;
}
