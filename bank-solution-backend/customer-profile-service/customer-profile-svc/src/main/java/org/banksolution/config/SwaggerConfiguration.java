package org.banksolution.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfiguration {

    @Bean
    public OpenAPI networkTopologyOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Customer Profile Service API")
                        .description("Customer Profile Service API")
                        .version("1.0.0"));
    }
}
