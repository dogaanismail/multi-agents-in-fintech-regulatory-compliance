package org.banksolution;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class CustomerProfileServiceDbMigrationApp {

    private CustomerProfileServiceDbMigrationApp() {
    }

    static void main(String[] args) {
        SpringApplication.run(CustomerProfileServiceDbMigrationApp.class, args);
    }
}
