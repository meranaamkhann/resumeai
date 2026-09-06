package com.resumeai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class ResumeIntelligenceApplication {
    public static void main(String[] args) {
        SpringApplication.run(ResumeIntelligenceApplication.class, args);
    }
}

