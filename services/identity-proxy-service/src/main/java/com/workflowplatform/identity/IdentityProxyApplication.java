package com.workflowplatform.identity;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class IdentityProxyApplication {

    public static void main(String[] args) {
        SpringApplication.run(IdentityProxyApplication.class, args);
    }
}
