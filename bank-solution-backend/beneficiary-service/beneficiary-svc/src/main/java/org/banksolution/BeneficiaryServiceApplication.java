package org.banksolution;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableJpaRepositories
@EnableFeignClients
public class BeneficiaryServiceApplication {

    private BeneficiaryServiceApplication() {
    }

    static void main(String[] args) {
        SpringApplication.run(BeneficiaryServiceApplication.class, args);
    }
}
