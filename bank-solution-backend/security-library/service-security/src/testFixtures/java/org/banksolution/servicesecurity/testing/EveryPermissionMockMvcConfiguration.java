package org.banksolution.servicesecurity.testing;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.MockMvcBuilderCustomizer;
import org.springframework.context.annotation.Bean;

import static org.banksolution.servicesecurity.testing.ServiceSecurityTestFixtures.createCallerJwtWithEveryPermission;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

@TestConfiguration
public class EveryPermissionMockMvcConfiguration {

    @Bean
    public MockMvcBuilderCustomizer everyPermissionCallerByDefault() {
        return mockMvcBuilder -> mockMvcBuilder.defaultRequest(get("/").with(createCallerJwtWithEveryPermission()));
    }
}
