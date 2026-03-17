package com.revature.revpay;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@EnableTransactionManagement
public class RevPayApplication {
    public static void main(String[] args) {
        SpringApplication.run(RevPayApplication.class, args);
    }
}