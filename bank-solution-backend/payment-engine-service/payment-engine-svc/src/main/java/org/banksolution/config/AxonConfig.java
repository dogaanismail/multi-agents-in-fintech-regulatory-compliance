package org.banksolution.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.kagkarlsson.scheduler.Scheduler;
import jakarta.persistence.EntityManager;
import org.axonframework.common.caching.Cache;
import org.axonframework.common.caching.WeakReferenceCache;
import org.axonframework.common.jpa.EntityManagerProvider;
import org.axonframework.common.jpa.SimpleEntityManagerProvider;
import org.axonframework.common.transaction.TransactionManager;
import org.axonframework.config.ConfigurationScopeAwareProvider;
import org.axonframework.config.EventProcessingConfigurer;
import org.axonframework.deadline.DeadlineManager;
import org.axonframework.deadline.DefaultDeadlineManagerSpanFactory;
import org.axonframework.deadline.dbscheduler.DbSchedulerDeadlineManager;
import org.axonframework.eventhandling.EventBus;
import org.axonframework.eventhandling.scheduling.EventScheduler;
import org.axonframework.eventhandling.scheduling.dbscheduler.DbSchedulerEventScheduler;
import org.axonframework.eventhandling.tokenstore.TokenStore;
import org.axonframework.eventhandling.tokenstore.jpa.JpaTokenStore;
import org.axonframework.eventsourcing.EventCountSnapshotTriggerDefinition;
import org.axonframework.eventsourcing.SnapshotTriggerDefinition;
import org.axonframework.eventsourcing.Snapshotter;
import org.axonframework.eventsourcing.eventstore.EventStorageEngine;
import org.axonframework.eventsourcing.eventstore.EventStore;
import org.axonframework.eventsourcing.eventstore.jpa.JpaEventStorageEngine;
import org.axonframework.messaging.ScopeAwareProvider;
import org.axonframework.modelling.saga.repository.SagaStore;
import org.axonframework.modelling.saga.repository.jpa.JpaSagaStore;
import org.axonframework.serialization.Serializer;
import org.axonframework.serialization.json.JacksonSerializer;
import org.axonframework.spring.eventsourcing.SpringAggregateSnapshotterFactoryBean;
import org.axonframework.spring.messaging.unitofwork.SpringTransactionManager;
import org.axonframework.tracing.SpanFactory;
import org.banksolution.domain.payment.saga.PaymentRiskSaga;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.concurrent.Executors;

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
    @Primary
    public ObjectMapper axonObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();

        // Java 8 date/time support
        objectMapper.registerModule(new JavaTimeModule());

        // Deserialization settings
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.configure(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES, false);

        // Serialization settings
        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);

        // Include only non-null values to reduce JSON size
        objectMapper.setDefaultPropertyInclusion(JsonInclude.Include.NON_NULL);

        return objectMapper;
    }

    @Bean
    @Primary
    public Serializer jacksonSerializer(
            ObjectMapper axonObjectMapper) {

        return JacksonSerializer.builder()
                .objectMapper(axonObjectMapper.copy())
                .defaultTyping()
                .build();
    }

    @Bean
    public TransactionManager axonTransactionManager(
            PlatformTransactionManager platformTransactionManager) {

        return new SpringTransactionManager(platformTransactionManager);
    }

    @Bean
    public EntityManagerProvider entityManagerProvider(
            EntityManager entityManager) {

        return new SimpleEntityManagerProvider(entityManager);
    }

    @Bean
    public DeadlineManager deadlineManager(
            Scheduler scheduler,
            org.axonframework.config.Configuration configuration,
            @Qualifier("eventSerializer") Serializer serializer,
            TransactionManager transactionManager,
            SpanFactory spanFactory) {

        ScopeAwareProvider scopeAwareProvider = new ConfigurationScopeAwareProvider(configuration);
        return DbSchedulerDeadlineManager.builder()
                .scheduler(scheduler)
                .scopeAwareProvider(scopeAwareProvider)
                .serializer(serializer)
                .transactionManager(transactionManager)
                .spanFactory(DefaultDeadlineManagerSpanFactory.builder()
                        .spanFactory(spanFactory)
                        .build())
                .startScheduler(true)
                .build();
    }

    @Bean
    public EventScheduler eventScheduler(
            @Qualifier("eventSerializer") final Serializer serializer,
            Scheduler scheduler,
            EventBus eventBus,
            TransactionManager transactionManager
    ) {
        return DbSchedulerEventScheduler.builder()
                .scheduler(scheduler)
                .serializer(serializer)
                .eventBus(eventBus)
                .transactionManager(transactionManager)
                .build();
    }

    @Bean
    public EventStorageEngine eventStorageEngine(
            Serializer serializer,
            @Qualifier("eventSerializer") Serializer eventSerializer,
            EntityManagerProvider entityManagerProvider,
            TransactionManager transactionManager) {

        return JpaEventStorageEngine.builder()
                .snapshotSerializer(serializer)
                .upcasterChain(parameter -> parameter)
                .eventSerializer(eventSerializer)
                .entityManagerProvider(entityManagerProvider)
                .transactionManager(transactionManager)
                .build();
    }

    @Bean
    public SpringAggregateSnapshotterFactoryBean snapshotter(
            EventStore eventStore,
            PlatformTransactionManager transactionManager) {

        SpringAggregateSnapshotterFactoryBean factoryBean = new SpringAggregateSnapshotterFactoryBean();
        factoryBean.setExecutor(Executors.newSingleThreadExecutor());
        factoryBean.setEventStore(eventStore);
        factoryBean.setTransactionManager(transactionManager);

        return factoryBean;
    }

    @Bean
    public SnapshotTriggerDefinition snapshotTriggerDefinition(
            Snapshotter snapshotter) {
        return new EventCountSnapshotTriggerDefinition(snapshotter, 10);
    }

    @Bean
    public Cache paymentCache() {
        return new WeakReferenceCache();
    }

    @Bean
    public TokenStore tokenStore(
            Serializer serializer,
            EntityManagerProvider entityManagerProvider) {

        return JpaTokenStore.builder()
                .entityManagerProvider(entityManagerProvider)
                .serializer(serializer)
                .build();
    }

    @Bean
    public SagaStore<Object> sagaStore(
            Serializer serializer,
            EntityManagerProvider entityManagerProvider) {

        return JpaSagaStore.builder()
                .entityManagerProvider(entityManagerProvider)
                .serializer(serializer)
                .build();
    }

    @Autowired
    public void configureSaga(EventProcessingConfigurer configurer) {
        configurer.registerSaga(PaymentRiskSaga.class);

        configurer.assignHandlerTypesMatching(
                "PaymentRiskSagaProcessor",
                clazz -> clazz.equals(PaymentRiskSaga.class)
        );

        configurer.registerSubscribingEventProcessor("PaymentRiskSagaProcessor");
    }

}
