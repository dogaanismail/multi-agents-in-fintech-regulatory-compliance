package org.banksolution;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class RiskEngineServiceDbMigrationApp {

    private RiskEngineServiceDbMigrationApp() {
    }

    static void main(String[] args) {
        SpringApplication.run(RiskEngineServiceDbMigrationApp.class, args);
    }
}
