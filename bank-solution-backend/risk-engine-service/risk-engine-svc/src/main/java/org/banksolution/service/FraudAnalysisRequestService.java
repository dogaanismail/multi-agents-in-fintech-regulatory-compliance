package org.banksolution.service;

import com.aml.fraud.FraudAnalysisRequestedEvent;
import com.aml.fraud.TransactionFeatures;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.banksolution.entity.RiskCheckRequestEntity;
import org.banksolution.infrastructure.messaging.kafka.producer.FraudAnalysisRequestedEventProducer;
import org.springframework.stereotype.Service;

import static org.banksolution.mapper.FraudAnalysisRequestedEventMapper.toAvroRequest;

@Service
@RequiredArgsConstructor
@Slf4j
public class FraudAnalysisRequestService {

    private final TransactionFeatureService transactionFeatureService;
    private final CustomerFeatureService customerFeatureService;
    private final NetworkFeatureService networkFeatureService;
    private final FraudAnalysisRequestedEventProducer fraudAnalysisRequestedEventProducer;

    public void processFraudAnalysisRequest(RiskCheckRequestEntity riskCheckRequestEntity) {
        TransactionFeatures transactionFeatures = transactionFeatureService.getTransactionFeatures(riskCheckRequestEntity);
        //TODO: Retrieve customer and network features

        String riskCheckRequestId = riskCheckRequestEntity.getId().toString();
        long requestTimestamp = riskCheckRequestEntity.getRequestTimestamp();
        FraudAnalysisRequestedEvent fraudAnalysisRequestedEvent = toAvroRequest(
                riskCheckRequestId,
                requestTimestamp,
                transactionFeatures,
                null,
                null);

        fraudAnalysisRequestedEventProducer.publishFraudAnalysisRequestedEvent(fraudAnalysisRequestedEvent);
    }

}
