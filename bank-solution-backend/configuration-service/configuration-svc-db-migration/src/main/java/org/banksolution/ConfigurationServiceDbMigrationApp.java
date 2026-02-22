package org.banksolution;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ConfigurationServiceDbMigrationApp {

    private ConfigurationServiceDbMigrationApp() {
    }

    static void main(String[] args) {
        SpringApplication.run(ConfigurationServiceDbMigrationApp.class, args);
    }
}
