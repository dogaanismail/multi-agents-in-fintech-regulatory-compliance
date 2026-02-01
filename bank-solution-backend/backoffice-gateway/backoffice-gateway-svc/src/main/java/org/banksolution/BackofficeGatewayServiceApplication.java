package org.banksolution;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class BackofficeGatewayServiceApplication {
    private BackofficeGatewayServiceApplication() {

    }

    static void main(String[] args) {
        SpringApplication.run(BackofficeGatewayServiceApplication.class, args);
    }
}
