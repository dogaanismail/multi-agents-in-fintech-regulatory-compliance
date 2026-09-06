package org.banksolution.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.kagkarlsson.scheduler.Scheduler;
import jakarta.persistence.EntityManager;
import org.axonframework.common.jpa.EntityManagerProvider;
import org.axonframework.common.jpa.SimpleEntityManagerProvider;
import org.axonframework.common.transaction.TransactionManager;
import org.axonframework.config.ConfigurationScopeAwareProvider;
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
import org.axonframework.eventsourcing.EventSourcingRepository;
import org.banksolution.domain.payment.aggregate.PaymentAggregate;
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
 * - Prevents expensive event store reads for active aggregates
 * - Weak references allow GC when memory is needed
 * <p>
 * 3. Sagas and Kafka publishers:
 * - Registered on pooled streaming processors in AxonEventProcessingConfig
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

        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.configure(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES, false);
        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        objectMapper.setDefaultPropertyInclusion(JsonInclude.Include.NON_NULL);
        objectMapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);

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

    @Bean
    public EventSourcingRepository<PaymentAggregate> paymentRepository(
            EventStore eventStore,
            SnapshotTriggerDefinition snapshotTriggerDefinition) {

        return EventSourcingRepository.builder(PaymentAggregate.class)
                .eventStore(eventStore)
                .snapshotTriggerDefinition(snapshotTriggerDefinition)
                .build();
    }

}
