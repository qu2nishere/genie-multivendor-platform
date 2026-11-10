package com.genie;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Genie — modular monolith: 11 module (auth, catalog, order, payment, vendor, shipping,
 * promotion, social, support, notification, admin) + common.
 */
@SpringBootApplication
public class GenieApplication {

    public static void main(String[] args) {
        SpringApplication.run(GenieApplication.class, args);
    }
}
