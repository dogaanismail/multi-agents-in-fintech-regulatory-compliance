package org.banksolution.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfiguration {

    @Bean
    public OpenAPI paymentEngineOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Payment Engine Service API")
                        .description("Event-Driven Payment Processing with Axon Framework - CQRS Query Endpoints")
                        .version("1.0.0"));
    }
}
