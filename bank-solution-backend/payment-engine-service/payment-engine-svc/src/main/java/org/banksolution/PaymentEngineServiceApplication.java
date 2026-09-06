package org.banksolution;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EntityScan(basePackages = {
        "org.banksolution.domain",
        "org.axonframework.eventsourcing.eventstore.jpa",
        "org.axonframework.eventhandling.tokenstore.jpa",
        "org.axonframework.modelling.saga.repository.jpa",
        "org.axonframework.eventhandling.deadletter.jpa"
})
@EnableKafka
@EnableAsync
@EnableScheduling
public class PaymentEngineServiceApplication {

    private PaymentEngineServiceApplication() {

    }

    static void main(String[] args) {
        SpringApplication.run(PaymentEngineServiceApplication.class, args);
    }
}