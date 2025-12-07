package org.banksolution;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableJpaRepositories
@EnableRetry
@EnableAsync
public class PaymentHistoryServiceApplication {

    private PaymentHistoryServiceApplication() {
    }

    static void main(String[] args) {
        SpringApplication.run(PaymentHistoryServiceApplication.class, args);
    }
}