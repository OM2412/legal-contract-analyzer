package com.omjadon.contractanalyzer;

import java.util.Map;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class LegalContractWebApplication {

    public static void main(String[] args) {
        SpringApplication application =
                new SpringApplication(LegalContractWebApplication.class);

        application.setDefaultProperties(Map.of(
                "server.address", "127.0.0.1",
                "server.port", "8080",
                "spring.servlet.multipart.max-file-size", "10MB",
                "spring.servlet.multipart.max-request-size", "20MB"
        ));

        application.run(args);
    }
}