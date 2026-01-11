package org.banksolution.service;

import com.aml.fraud.FraudAnalysisRequestedEvent;
import com.aml.fraud.TransactionFeatures;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.banksolution.entity.RiskCheckRequestEntity;
import org.banksolution.infrastructure.messaging.kafka.producer.FraudAnalysisRequestedEventProducer;
import org.banksolution.mapper.FraudAnalysisRequestedEventMapper;
import org.springframework.stereotype.Service;

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

        FraudAnalysisRequestedEvent event = FraudAnalysisRequestedEventMapper.toAvroRequest(
                riskCheckRequestEntity.getId().toString(),
                riskCheckRequestEntity.getRequestTimestamp(),
                transactionFeatures,
                null,
                null);

        fraudAnalysisRequestedEventProducer.publishFraudAnalysisRequestedEvent(event);
    }

}
