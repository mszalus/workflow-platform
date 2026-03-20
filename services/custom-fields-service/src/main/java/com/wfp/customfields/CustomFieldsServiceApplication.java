package com.wfp.customfields;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"com.wfp.customfields", "com.wfp.security", "com.wfp.common"})
public class CustomFieldsServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CustomFieldsServiceApplication.class, args);
    }
}
