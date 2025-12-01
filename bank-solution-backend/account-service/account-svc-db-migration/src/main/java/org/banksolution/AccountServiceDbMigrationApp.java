package org.banksolution;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class AccountServiceDbMigrationApp {

    private AccountServiceDbMigrationApp() {
    }

    static void main(String[] args) {
        SpringApplication.run(AccountServiceDbMigrationApp.class, args);
    }
}
