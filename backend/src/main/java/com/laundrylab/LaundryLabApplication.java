package com.laundrylab;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class LaundryLabApplication {
    public static void main(String[] args) { SpringApplication.run(LaundryLabApplication.class, args); }
}
