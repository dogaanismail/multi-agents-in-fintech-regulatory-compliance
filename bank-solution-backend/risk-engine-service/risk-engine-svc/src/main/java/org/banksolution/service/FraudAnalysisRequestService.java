package org.banksolution.service;

import com.aml.fraud.FraudAnalysisRequestedEvent;
import com.aml.fraud.TransactionFeatures;
import com.aml.fraud.NetworkFeatures;
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
        NetworkFeatures networkFeatures = networkFeatureService.getNetworkFeatures(riskCheckRequestEntity.getSourceAccountId());

        String riskCheckRequestId = riskCheckRequestEntity.getId().toString();
        long requestTimestamp = riskCheckRequestEntity.getRequestTimestamp();
        FraudAnalysisRequestedEvent fraudAnalysisRequestedEvent = FraudAnalysisRequestedEventMapper.toAvroRequest(
                riskCheckRequestId,
                requestTimestamp,
                transactionFeatures,
                null,
                networkFeatures);

        fraudAnalysisRequestedEventProducer.publishFraudAnalysisRequestedEvent(fraudAnalysisRequestedEvent);
    }

}
