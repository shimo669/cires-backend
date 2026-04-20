package com.cires.ciresbackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CiresBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(CiresBackendApplication.class, args);
    }

}
