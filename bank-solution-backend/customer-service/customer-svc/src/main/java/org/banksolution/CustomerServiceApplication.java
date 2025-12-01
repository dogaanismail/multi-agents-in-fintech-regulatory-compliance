package org.banksolution;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableJpaRepositories
public class CustomerServiceApplication {

    private CustomerServiceApplication() {
    }

    static void main(String[] args) {
        SpringApplication.run(CustomerServiceApplication.class, args);
    }
}