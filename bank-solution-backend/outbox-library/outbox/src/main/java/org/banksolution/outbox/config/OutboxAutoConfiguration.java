package org.banksolution.outbox.config;

import com.github.kagkarlsson.scheduler.task.helper.RecurringTask;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.propagation.Propagator;

import java.time.Clock;

import org.banksolution.outbox.publisher.OutboxEventPublisher;
import org.banksolution.outbox.relay.OutboxEventCleaner;
import org.banksolution.outbox.relay.OutboxEventMetrics;
import org.banksolution.outbox.relay.OutboxEventRelay;
import org.banksolution.outbox.relay.OutboxKafkaSender;
import org.banksolution.outbox.relay.OutboxRetryBackoff;
import org.banksolution.outbox.repository.OutboxEventRepository;
import org.banksolution.outbox.scheduling.OutboxSchedulerTasks;
import org.banksolution.outbox.serialization.OutboxPayloadSerializer;
import org.banksolution.outbox.tracing.OutboxTraceContextPropagator;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

@AutoConfiguration
@ConditionalOnProperty(prefix = "outbox", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(OutboxProperties.class)
public class OutboxAutoConfiguration {

    @Bean
    public OutboxPayloadSerializer outboxPayloadSerializer(
            OutboxProperties outboxProperties,
            Environment environment) {

        return new OutboxPayloadSerializer(
                resolveRequiredSetting(
                        outboxProperties.getKafka().getSchemaRegistryUrl(),
                        environment,
                        "spring.kafka.schema-registry.url",
                        "outbox.kafka.schema-registry-url"));
    }

    @Bean
    public OutboxKafkaSender outboxKafkaSender(
            OutboxProperties outboxProperties,
            Environment environment,
            ObjectProvider<ObservationRegistry> observationRegistry) {

        return new OutboxKafkaSender(
                resolveRequiredSetting(outboxProperties.getKafka().getBootstrapServers(), environment,
                        "spring.kafka.bootstrap-servers", "outbox.kafka.bootstrap-servers"),
                outboxProperties.getKafka().getSendTimeout(),
                observationRegistry.getIfAvailable());
    }

    @Bean
    public OutboxTraceContextPropagator outboxTraceContextPropagator(
            ObjectProvider<Tracer> tracer,
            ObjectProvider<Propagator> propagator) {

        return new OutboxTraceContextPropagator(tracer.getIfAvailable(), propagator.getIfAvailable());
    }

    @Bean
    public OutboxEventRelay outboxEventRelay(
            OutboxEventRepository outboxEventRepository,
            OutboxKafkaSender outboxKafkaSender,
            OutboxTraceContextPropagator outboxTraceContextPropagator,
            PlatformTransactionManager platformTransactionManager,
            ObjectProvider<MeterRegistry> meterRegistry,
            ObjectProvider<Clock> clock,
            OutboxProperties outboxProperties) {

        OutboxProperties.Relay relayProperties = outboxProperties.getRelay();
        return new OutboxEventRelay(
                outboxEventRepository,
                outboxKafkaSender,
                outboxTraceContextPropagator,
                new OutboxRetryBackoff(relayProperties.getInitialBackoff(), relayProperties.getMaxBackoff()),
                new OutboxEventMetrics(meterRegistry.getIfAvailable()),
                toRequiresNewTransactionTemplate(platformTransactionManager),
                clock.getIfAvailable(Clock::systemUTC),
                relayProperties.getBatchSize(),
                relayProperties.getMaxAttempts());
    }

    @Bean
    public OutboxEventPublisher outboxEventPublisher(
            OutboxEventRepository outboxEventRepository,
            OutboxPayloadSerializer outboxPayloadSerializer,
            OutboxTraceContextPropagator outboxTraceContextPropagator,
            OutboxEventRelay outboxEventRelay,
            ObjectProvider<Clock> clock,
            OutboxProperties outboxProperties) {

        return new OutboxEventPublisher(
                outboxEventRepository,
                outboxPayloadSerializer,
                outboxTraceContextPropagator,
                outboxEventRelay,
                toImmediatePublishExecutor(outboxProperties.getRelay()),
                clock.getIfAvailable(Clock::systemUTC));
    }

    @Bean
    public OutboxEventCleaner outboxEventCleaner(
            OutboxEventRepository outboxEventRepository,
            PlatformTransactionManager platformTransactionManager,
            ObjectProvider<Clock> clock,
            OutboxProperties outboxProperties) {

        return new OutboxEventCleaner(
                outboxEventRepository,
                toRequiresNewTransactionTemplate(platformTransactionManager),
                clock.getIfAvailable(Clock::systemUTC),
                outboxProperties.getCleanup().getRetention()
        );
    }

    @Bean
    public RecurringTask<Void> outboxRelayTask(
            OutboxEventRelay outboxEventRelay,
            OutboxProperties outboxProperties) {

        return OutboxSchedulerTasks.createOutboxRelayTask(
                outboxEventRelay,
                outboxProperties.getRelay().getPollingInterval()
        );
    }

    @Bean
    public RecurringTask<Void> outboxCleanupTask(
            OutboxEventCleaner outboxEventCleaner,
            OutboxProperties outboxProperties) {

        return OutboxSchedulerTasks.createOutboxCleanupTask(
                outboxEventCleaner,
                outboxProperties.getCleanup().getInterval()
        );
    }

    private static TransactionTemplate toRequiresNewTransactionTemplate(
            PlatformTransactionManager platformTransactionManager) {

        TransactionTemplate transactionTemplate = new TransactionTemplate(platformTransactionManager);
        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        return transactionTemplate;
    }

    private static ThreadPoolTaskExecutor toImmediatePublishExecutor(
            OutboxProperties.Relay relayProperties) {

        if (!relayProperties.isPublishImmediately()) {
            return null;
        }

        ThreadPoolTaskExecutor immediatePublishExecutor = new ThreadPoolTaskExecutor();
        immediatePublishExecutor.setThreadNamePrefix("outbox-publish-");
        immediatePublishExecutor.setCorePoolSize(relayProperties.getImmediatePublishThreads());
        immediatePublishExecutor.setMaxPoolSize(relayProperties.getImmediatePublishThreads());
        immediatePublishExecutor.setQueueCapacity(relayProperties.getImmediatePublishQueueCapacity());
        immediatePublishExecutor.initialize();

        return immediatePublishExecutor;
    }

    private static String resolveRequiredSetting(
            String explicitValue,
            Environment environment,
            String fallbackProperty,
            String outboxProperty) {

        String resolvedValue = explicitValue != null ? explicitValue : environment.getProperty(fallbackProperty);
        if (resolvedValue == null || resolvedValue.isBlank()) {
            throw new IllegalStateException("Outbox needs " + outboxProperty + " or " + fallbackProperty + " to be set");
        }

        return resolvedValue;
    }
}
