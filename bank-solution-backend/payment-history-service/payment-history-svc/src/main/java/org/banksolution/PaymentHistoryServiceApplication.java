package org.banksolution;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableJpaRepositories
public class PaymentHistoryServiceApplication {

    private PaymentHistoryServiceApplication() {
    }

    static void main(String[] args) {
        SpringApplication.run(PaymentHistoryServiceApplication.class, args);
    }
}