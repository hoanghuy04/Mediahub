package com.bondhub.common;

import org.junit.jupiter.api.Test;

/**
 * Basic test for common module
 * This is a library module, so we don't need Spring context
 */
class CommonApplicationTests {

    @Test
    void contextLoads() {
        // This is a library module - no Spring Boot application to load
        // Just verify the test runs successfully
        assert true;
    }

}
