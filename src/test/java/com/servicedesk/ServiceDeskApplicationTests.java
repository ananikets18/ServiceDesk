package com.servicedesk;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Basic test that makes sure the Spring context loads correctly.
 * It needs a running PostgreSQL database (see application.properties).
 */
@SpringBootTest
class ServiceDeskApplicationTests {

    @Test
    void contextLoads() {
        // If the application context fails to start, this test will fail.
    }
}
