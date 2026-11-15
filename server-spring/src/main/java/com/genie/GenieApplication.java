package com.genie;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Genie — modular monolith: 11 module (auth, catalog, order, payment, vendor, shipping,
 * promotion, social, support, notification, admin) + common.
 */
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)   // xác thực bằng JWT riêng — không cần user mặc định
@ConfigurationPropertiesScan
public class GenieApplication {

    public static void main(String[] args) {
        SpringApplication.run(GenieApplication.class, args);
    }
}
