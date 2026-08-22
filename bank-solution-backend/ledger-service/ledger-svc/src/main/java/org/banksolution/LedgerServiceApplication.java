package org.banksolution;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.kafka.annotation.EnableKafka;

@SpringBootApplication
@ConfigurationPropertiesScan
@EnableKafka
public class LedgerServiceApplication {

    private LedgerServiceApplication() {
    }

    static void main(String[] args) {
        SpringApplication.run(LedgerServiceApplication.class, args);
    }
}
