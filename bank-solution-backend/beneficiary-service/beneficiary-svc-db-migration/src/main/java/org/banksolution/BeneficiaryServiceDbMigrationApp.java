package org.banksolution;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class BeneficiaryServiceDbMigrationApp {

    private BeneficiaryServiceDbMigrationApp() {
    }

    static void main(String[] args) {
        SpringApplication.run(BeneficiaryServiceDbMigrationApp.class, args);
    }
}
