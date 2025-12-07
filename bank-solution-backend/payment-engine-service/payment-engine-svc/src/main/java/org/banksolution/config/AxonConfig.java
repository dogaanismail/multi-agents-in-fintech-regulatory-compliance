package org.banksolution.config;

import org.axonframework.commandhandling.CommandBus;
import org.axonframework.commandhandling.SimpleCommandBus;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.commandhandling.gateway.DefaultCommandGateway;
import org.axonframework.common.caching.Cache;
import org.axonframework.common.caching.WeakReferenceCache;
import org.axonframework.common.transaction.TransactionManager;
import org.axonframework.eventsourcing.EventCountSnapshotTriggerDefinition;
import org.axonframework.eventsourcing.SnapshotTriggerDefinition;
import org.axonframework.eventsourcing.Snapshotter;
import org.axonframework.messaging.interceptors.BeanValidationInterceptor;
import org.axonframework.messaging.interceptors.LoggingInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Axon Framework Configuration
 * <p>
 * Key architectural decisions implemented:
 * <p>
 * 1. Snapshots:
 * - Trigger snapshot after every event (eventCountSnapshotTriggerDefinition)
 * - Snapshots published to Kafka via SnapshotEventPublisher for payment-history service
 * - Uses Axon's native snapshot mechanism instead of custom implementation
 * <p>
 * 2. Caching:
 * - WeakReferenceCache for in-memory aggregate caching
 * - Prevents expensive event store reads for active aggregates
 * - Weak references allow GC when memory is needed
 * <p>
 * 3. Sagas:
 * - PaymentRiskSaga: Orchestrates payment risk workflow
 * Replaces previous "process manager" logic scattered across handlers
 * <p>
 * 4. Command Bus:
 * - Bean validation interceptor for command validation
 * - Logging interceptor for debugging and audit trail
 */
@Configuration
public class AxonConfig {

    @Bean
    public CommandBus commandBus(TransactionManager transactionManager) {
        SimpleCommandBus commandBus = SimpleCommandBus.builder()
                .transactionManager(transactionManager)
                .build();

        commandBus.registerHandlerInterceptor(new BeanValidationInterceptor<>());
        commandBus.registerDispatchInterceptor(new LoggingInterceptor<>());

        return commandBus;
    }

    @Bean
    public CommandGateway commandGateway(CommandBus commandBus) {
        return DefaultCommandGateway.builder()
                .commandBus(commandBus)
                .build();
    }

    /**
     * Configure Axon to create a snapshot after every single event.
     * This ensures we always have the latest state available for publishing to Kafka.
     */
    @Bean
    public SnapshotTriggerDefinition snapshotTriggerDefinition(Snapshotter snapshotter) {
        return new EventCountSnapshotTriggerDefinition(snapshotter, 1);
    }

    /**
     * Configure in-memory caching for Payment aggregates.
     * <p>
     * Benefits:
     * - Prevents expensive event store reads for recently accessed aggregates
     * - WeakReferenceCache uses weak references, allowing GC when memory is tight
     * - Ideal for aggregates with consistent command routing (same aggregate = same instance)
     * <p>
     * Production considerations:
     * - For distributed systems, ensure commands are routed consistently (same aggregate ID → same node)
     * - Consider EhCache or JCache for more advanced caching strategies
     * - Monitor cache hit rates to validate effectiveness
     */
    @Bean
    public Cache paymentCache() {
        return new WeakReferenceCache();
    }
}
