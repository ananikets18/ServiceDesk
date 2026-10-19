package com.servicedesk;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main entry point of the ServiceDesk application.
 *
 * Running this class starts the embedded server, connects to PostgreSQL
 * and makes all of our REST endpoints available.
 */
@SpringBootApplication
public class ServiceDeskApplication {

    public static void main(String[] args) {
        SpringApplication.run(ServiceDeskApplication.class, args);
    }
}
