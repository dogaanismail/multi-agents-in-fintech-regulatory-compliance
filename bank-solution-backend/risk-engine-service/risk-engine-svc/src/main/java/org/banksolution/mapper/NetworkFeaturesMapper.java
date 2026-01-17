package org.banksolution.mapper;

import com.aml.fraud.NetworkFeatures;
import lombok.experimental.UtilityClass;
import org.banksolution.integration.networktopology.dto.NetworkFeatureResponse;

@UtilityClass
public class NetworkFeaturesMapper {

    public NetworkFeatures toAvroNetworkFeatures(NetworkFeatureResponse response) {
        return NetworkFeatures.newBuilder()
                .setAccountId(response.getAccountId())
                .setInDegree(response.getInDegree())
                .setOutDegree(response.getOutDegree())
                .setDegreeCentrality(response.getDegreeCentrality())
                .setInDegreeCentrality(response.getInDegreeCentrality())
                .setOutDegreeCentrality(response.getOutDegreeCentrality())
                .setBetweennessCentrality(response.getBetweennessCentrality())
                .setClosenessCentrality(response.getClosenessCentrality())
                .setPagerank(response.getPagerank())
                .setEigenvectorCentrality(response.getEigenvectorCentrality())
                .setClusteringCoefficient(response.getClusteringCoefficient())
                .setCommunity(response.getCommunity())
                .build();
    }

    public NetworkFeatures getDefaultNetworkFeatures(String accountId) {
        return NetworkFeatures.newBuilder()
                .setAccountId(accountId)
                .setInDegree(0)
                .setOutDegree(0)
                .setDegreeCentrality(0.0)
                .setInDegreeCentrality(0.0)
                .setOutDegreeCentrality(0.0)
                .setBetweennessCentrality(0.0)
                .setClosenessCentrality(0.0)
                .setPagerank(0.0)
                .setEigenvectorCentrality(0.0)
                .setClusteringCoefficient(0.0)
                .setCommunity(0)
                .build();
    }
}
