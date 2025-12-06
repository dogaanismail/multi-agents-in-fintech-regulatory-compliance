package org.banksolution;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class PaymentServiceDbMigrationApp {

    private PaymentServiceDbMigrationApp() {
    }

    static void main(String[] args) {
        SpringApplication.run(PaymentServiceDbMigrationApp.class, args);
    }
}
