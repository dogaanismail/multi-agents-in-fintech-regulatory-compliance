package org.banksolution.kafka.producer;

import com.aml.fraud.FraudDetectionRequest;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.banksolution.event.FraudCheckRequestedEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class FraudDetectionRequestProducer {

    private final KafkaTemplate<@NonNull String, @NonNull FraudDetectionRequest> fraudDetectionRequestKafkaTemplate;

    @Value("${kafka.topics.fraud-detection-request}")
    private String fraudDetectionRequestTopic;

    public void publishFraudCheckRequest(FraudCheckRequestedEvent event) {
        log.info("Publishing FraudDetectionRequest for payment: {}", event.getPaymentId());

        FraudDetectionRequest request = FraudDetectionRequest.newBuilder()
                .setRequestId(UUID.randomUUID().toString())
                .setTransactionId(event.getReferenceNumber())
                .setTransactionFeatures(null)
                .setCustomerFeatures(null)
                .setNetworkFeatures(null)
                .build();

        fraudDetectionRequestKafkaTemplate.send(
                fraudDetectionRequestTopic,
                event.getReferenceNumber(),
                request
        );

        log.info("Published FraudDetectionRequest: requestId:{}, transactionId:{}",
                request.getRequestId(),
                request.getTransactionId());
    }
}
