package org.banksolution.config;

import com.tigerbeetle.Client;
import com.tigerbeetle.UInt128;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class TigerBeetleConfig {

    @Bean(destroyMethod = "close")
    public Client tigerBeetleClient(TigerBeetleProperties properties) {
        String[] addresses = properties.addresses().toArray(String[]::new);

        log.info("Connecting to TigerBeetle cluster {} at {}", properties.clusterId(), properties.addresses());
        Client client = new Client(UInt128.asBytes(properties.clusterId()), addresses);
        log.info("TigerBeetle client initialised");

        return client;
    }
}
