package org.banksolution;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class PaymentEngineServiceDbMigrationApp {

    private PaymentEngineServiceDbMigrationApp() {
    }

    static void main(String[] args) {
        SpringApplication.run(PaymentEngineServiceDbMigrationApp.class, args);
    }
}
