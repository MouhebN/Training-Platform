package com.training.platform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class TrainingPlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(TrainingPlatformApplication.class, args);
    }
}
