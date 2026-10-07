package com.deepblue.rescue;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableJpaRepositories(basePackages = "com.deepblue.rescue.repository")
public class DeepblueRescueApplication {

    public static void main(String[] args) {
        SpringApplication.run(DeepblueRescueApplication.class, args);
    }
}