package com.dailymark;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class DailyMarkApplication {
    public static void main(String[] args) {
        SpringApplication.run(DailyMarkApplication.class, args);
    }
}
