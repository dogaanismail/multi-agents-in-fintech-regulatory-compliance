package org.banksolution.config;

import java.time.Clock;
import java.time.ZoneId;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GeneralConfig {

    @Bean
    public Clock clock() {
        return Clock.system(ZoneId.of("Europe/Tallinn"));
    }
}
