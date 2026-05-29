package com.fpm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class FpmApplication {
    public static void main(String[] args) {
        SpringApplication.run(FpmApplication.class, args);
    }
}
