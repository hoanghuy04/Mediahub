package com.bondhub.authservice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import org.junit.jupiter.api.Disabled;

@Disabled("Skipped context loads test because it requires running external databases (MongoDB, Redis, Eureka, etc.)")
@SpringBootTest
class AuthServiceApplicationTests {

    @Test
    void contextLoads() {
    }

}
