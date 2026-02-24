package com.wallaceespindola.resilience4jdemo.service;

import com.wallaceespindola.resilience4jdemo.client.SimulatedDownstreamClient;
import com.wallaceespindola.resilience4jdemo.config.CacheConfig;
import io.github.resilience4j.cache.Cache;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.cache.configuration.MutableConfiguration;
import javax.cache.spi.CachingProvider;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CacheDemoService Tests")
class CacheDemoServiceTest {

    @Mock private SimulatedDownstreamClient client;

    private javax.cache.Cache<String, String> jCache;
    private Cache<String, String>             r4jCache;
    private CacheConfig                       cacheConfig;
    private CacheDemoService                  service;

    @BeforeEach
    void setUp() {
        CachingProvider provider = Caching.getCachingProvider(
                "com.github.benmanes.caffeine.jcache.spi.CaffeineCachingProvider");
        CacheManager manager = provider.getCacheManager();
        MutableConfiguration<String, String> cfg = new MutableConfiguration<String, String>()
                .setTypes(String.class, String.class)
                .setStoreByValue(false);
        // unique name per test to avoid JCache name collisions
        jCache   = manager.createCache("testMeta-" + UUID.randomUUID(), cfg);
        r4jCache = Cache.of(jCache);
        cacheConfig = new CacheConfig();
        service  = new CacheDemoService(r4jCache, jCache, client, cacheConfig);
    }

    @AfterEach
    void tearDown() {
        jCache.close();
    }

    @Test
    @DisplayName("getMetadata on cache miss fetches from client")
    void getMetadata_cacheMiss_callsClient() {
        when(client.fetchMetadata("region")).thenReturn("EU-WEST-1");

        String result = service.getMetadata("region");

        assertThat(result).isEqualTo("EU-WEST-1");
        verify(client).fetchMetadata("region");
    }

    @Test
    @DisplayName("getMetadata on cache hit does not call client again")
    void getMetadata_cacheHit_doesNotCallClient() {
        jCache.put("env", "demo");   // prime the cache

        String result = service.getMetadata("env");

        assertThat(result).isEqualTo("demo");
        verify(client, never()).fetchMetadata(anyString());
    }

    @Test
    @DisplayName("getAllMetadata returns all five keys")
    void getAllMetadata_returnsAllFiveKeys() {
        when(client.fetchMetadata(anyString())).thenReturn("val");

        Map<String, String> result = service.getAllMetadata();

        assertThat(result).containsKeys("region", "env", "version", "owner", "sla");
        assertThat(result).hasSize(5);
    }

    @Test
    @DisplayName("clearCache clears the JCache and resets counters")
    void clearCache_resetsCounters() {
        cacheConfig.cacheHits.set(10);
        cacheConfig.cacheMisses.set(5);
        jCache.put("region", "EU-WEST-1");

        service.clearCache();

        assertThat(jCache.get("region")).isNull();
        assertThat(cacheConfig.cacheHits.get()).isEqualTo(0);
        assertThat(cacheConfig.cacheMisses.get()).isEqualTo(0);
    }

    @Test
    @DisplayName("getStats returns hits and misses from CacheConfig")
    void getStats_returnsCurrentCounters() {
        cacheConfig.cacheHits.set(7);
        cacheConfig.cacheMisses.set(3);

        Map<String, Long> stats = service.getStats();

        assertThat(stats.get("hits")).isEqualTo(7L);
        assertThat(stats.get("misses")).isEqualTo(3L);
    }

    @Test
    @DisplayName("getAllMetadata handles client error gracefully")
    void getAllMetadata_clientError_returnsErrorEntry() {
        when(client.fetchMetadata(anyString())).thenThrow(new RuntimeException("downstream down"));

        Map<String, String> result = service.getAllMetadata();

        assertThat(result).allSatisfy((k, v) -> assertThat(v).startsWith("ERROR:"));
    }
}
