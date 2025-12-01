package org.banksolution;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class NetworkTopologyServiceDbMigrationApp {

    private NetworkTopologyServiceDbMigrationApp() {
    }

    static void main(String[] args) {
        SpringApplication.run(NetworkTopologyServiceDbMigrationApp.class, args);
    }
}
