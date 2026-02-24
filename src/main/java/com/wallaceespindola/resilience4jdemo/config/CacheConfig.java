package com.wallaceespindola.resilience4jdemo.config;

import com.github.benmanes.caffeine.jcache.configuration.CaffeineConfiguration;
import io.github.resilience4j.cache.Cache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.cache.Caching;
import javax.cache.spi.CachingProvider;
import java.util.OptionalLong;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Configures Spring Cache (Caffeine) and the Resilience4J Cache wrapper.
 *
 * <p>The Resilience4J {@link Cache} instance wraps a JCache-backed Caffeine cache.
 * It exposes hit/miss counters accessible from the dashboard.
 */
@Configuration
@EnableCaching
@Slf4j
public class CacheConfig {

    public static final String METADATA_CACHE = "metadataCache";

    // Counters for dashboard display
    public final AtomicLong cacheHits   = new AtomicLong(0);
    public final AtomicLong cacheMisses = new AtomicLong(0);

    /**
     * Spring CacheManager backed by Caffeine (used by @Cacheable on non-R4J paths).
     */
    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager(METADATA_CACHE);
        manager.setCacheSpecification("maximumSize=500,expireAfterWrite=30s");
        return manager;
    }

    /**
     * JCache instance backed by Caffeine, used by the Resilience4J Cache wrapper.
     * TTL: 30 seconds, max 500 entries.
     */
    @Bean
    public javax.cache.Cache<String, String> jMetadataCache() {
        CachingProvider provider = Caching.getCachingProvider(
                "com.github.benmanes.caffeine.jcache.spi.CaffeineCachingProvider");
        javax.cache.CacheManager cm = provider.getCacheManager();

        // Remove existing cache if it exists (for test restarts)
        cm.destroyCache(METADATA_CACHE);

        CaffeineConfiguration<String, String> config = new CaffeineConfiguration<>();
        config.setMaximumSize(OptionalLong.of(500));
        config.setExpireAfterWrite(OptionalLong.of(TimeUnit.SECONDS.toNanos(30)));
        config.setStoreByValue(false);

        return cm.createCache(METADATA_CACHE, config);
    }

    /**
     * Resilience4J Cache instance wrapping the JCache.
     * Attaches event listeners to count hits and misses.
     */
    @Bean
    public Cache<String, String> r4jMetadataCache(javax.cache.Cache<String, String> jMetadataCache) {
        Cache<String, String> r4jCache = Cache.of(jMetadataCache);
        r4jCache.getEventPublisher()
                .onCacheHit(e   -> cacheHits.incrementAndGet())
                .onCacheMiss(e  -> cacheMisses.incrementAndGet())
                .onError(e      -> log.warn("Cache error: {}", e.getThrowable().getMessage()));
        return r4jCache;
    }
}
