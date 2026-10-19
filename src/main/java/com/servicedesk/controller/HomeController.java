package com.servicedesk.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Small controller used to check that the application is running.
 * We can refine or remove this once the real ticket endpoints exist.
 */
@RestController
public class HomeController {

    @GetMapping("/")
    public String home() {
        return "ServiceDesk API is up and running!";
    }

    @GetMapping("/api/v1/health")
    public String health() {
        return "OK";
    }
}
