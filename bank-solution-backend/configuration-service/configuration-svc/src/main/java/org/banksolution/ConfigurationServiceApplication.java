package org.banksolution;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableJpaRepositories
@EnableCaching
public class ConfigurationServiceApplication {

    private ConfigurationServiceApplication() {
    }

    static void main(String[] args) {
        SpringApplication.run(ConfigurationServiceApplication.class, args);
    }
}
