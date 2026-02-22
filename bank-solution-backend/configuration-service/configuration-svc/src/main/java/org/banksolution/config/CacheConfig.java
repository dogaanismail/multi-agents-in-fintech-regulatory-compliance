package org.banksolution.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
public class CacheConfig {

    public static final String CONFIGURATIONS_CACHE = "configurations";
    public static final String CONFIGURATION_BY_KEY_CACHE = "configurationByKey";
    public static final String CONFIGURATIONS_BY_CATEGORY_CACHE = "configurationsByCategory";

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(
                CONFIGURATIONS_CACHE,
                CONFIGURATION_BY_KEY_CACHE,
                CONFIGURATIONS_BY_CATEGORY_CACHE
        );
        cacheManager.setCaffeine(caffeineCacheBuilder());
        return cacheManager;
    }

    private Caffeine<Object, Object> caffeineCacheBuilder() {
        return Caffeine.newBuilder()
                .initialCapacity(50)
                .maximumSize(500)
                .expireAfterWrite(10, TimeUnit.MINUTES)
                .recordStats();
    }
}
