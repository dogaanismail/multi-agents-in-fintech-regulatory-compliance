package org.banksolution;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.kafka.annotation.EnableKafka;

@SpringBootApplication
@EnableJpaRepositories
@EnableFeignClients
@EnableKafka
public class RiskEngineServiceApplication {

    private RiskEngineServiceApplication() {
    }

    static void main(String[] args) {
        SpringApplication.run(RiskEngineServiceApplication.class, args);
    }
}