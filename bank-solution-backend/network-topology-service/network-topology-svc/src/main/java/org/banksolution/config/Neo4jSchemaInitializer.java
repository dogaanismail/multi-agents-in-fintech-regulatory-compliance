package org.banksolution.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class Neo4jSchemaInitializer {

    private final Driver neo4jDriver;

    @EventListener(ApplicationReadyEvent.class)
    public void initializeSchema() {
        try (Session session = neo4jDriver.session()) {
            session.run("""
                    CREATE CONSTRAINT account_id_unique IF NOT EXISTS
                    FOR (a:Account) REQUIRE a.accountId IS UNIQUE
                    """);

            session.run("""
                    CREATE INDEX account_customer_id IF NOT EXISTS
                    FOR (a:Account) ON (a.customerId)
                    """);

            log.info("Neo4j schema initialized successfully");
        } catch (Exception e) {
            log.warn("Schema initialization skipped or failed: {}", e.getMessage());
        }
    }
}
