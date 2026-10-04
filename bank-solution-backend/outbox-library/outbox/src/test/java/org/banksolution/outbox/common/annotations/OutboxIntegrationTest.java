package org.banksolution.outbox.common.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.banksolution.outbox.OutboxTestApplication;
import org.banksolution.outbox.common.initializers.KafkaInitializer;
import org.banksolution.outbox.common.initializers.PostgreSQLInitializer;
import org.junit.jupiter.api.Tag;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Tag("integration")
@SpringBootTest(classes = OutboxTestApplication.class)
@ContextConfiguration(initializers = {PostgreSQLInitializer.class, KafkaInitializer.class})
@ActiveProfiles("test")
public @interface OutboxIntegrationTest {
}
