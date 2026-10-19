package com.servicedesk;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Basic test that makes sure the Spring context loads correctly.
 * It needs a running PostgreSQL database (see application.properties).
 * The "test" profile supplies a fixed JWT secret.
 */
@SpringBootTest
@ActiveProfiles("test")
class ServiceDeskApplicationTests {

    @Test
    void contextLoads() {
        // If the application context fails to start, this test will fail.
    }
}
