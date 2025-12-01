package org.banksolution;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class PaymentHistoryServiceDbMigrationApp {

    private PaymentHistoryServiceDbMigrationApp() {
    }

    static void main(String[] args) {
        SpringApplication.run(PaymentHistoryServiceDbMigrationApp.class, args);
    }
}
