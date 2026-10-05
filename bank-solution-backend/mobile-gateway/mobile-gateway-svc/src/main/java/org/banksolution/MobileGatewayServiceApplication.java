package org.banksolution;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class MobileGatewayServiceApplication {

    static void main(String[] args) {
        SpringApplication.run(MobileGatewayServiceApplication.class, args);
    }
}
