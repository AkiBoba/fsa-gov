package com.example.fsa_gov;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class FsaGovApplication {

    public static void main(String[] args) {
        SpringApplication.run(FsaGovApplication.class, args);
    }

}
