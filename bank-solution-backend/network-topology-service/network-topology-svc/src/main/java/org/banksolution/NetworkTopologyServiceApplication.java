package org.banksolution;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableFeignClients
@EnableScheduling
@EnableKafka
public class NetworkTopologyServiceApplication {

    private NetworkTopologyServiceApplication() {
    }

    static void main(String[] args) {
        SpringApplication.run(NetworkTopologyServiceApplication.class, args);
    }
}