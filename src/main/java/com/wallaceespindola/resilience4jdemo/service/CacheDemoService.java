package com.wallaceespindola.resilience4jdemo.service;

import com.wallaceespindola.resilience4jdemo.client.SimulatedDownstreamClient;
import com.wallaceespindola.resilience4jdemo.config.CacheConfig;
import io.github.resilience4j.cache.Cache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Demonstrates the Resilience4J Cache pattern.
 *
 * <p>The Cache wraps the expensive {@code fetchMetadata} downstream call.
 * On the first call the value is fetched (miss), on subsequent calls it is
 * returned from the Caffeine-backed JCache (hit).
 *
 * <p>Enable fault injection (e.g. errorRate=80%) and observe that cached values
 * are served even when the downstream would fail — the cache protects against
 * repeated expensive/failing calls.
 */
@Service
@Slf4j
public class CacheDemoService {

    private final Cache<String, String>             r4jCache;
    private final javax.cache.Cache<String, String> jCache;
    private final SimulatedDownstreamClient         client;
    private final CacheConfig                       cacheConfig;

    public CacheDemoService(Cache<String, String> r4jCache,
                            javax.cache.Cache<String, String> jMetadataCache,
                            SimulatedDownstreamClient client,
                            CacheConfig cacheConfig) {
        this.r4jCache    = r4jCache;
        this.jCache      = jMetadataCache;
        this.client      = client;
        this.cacheConfig = cacheConfig;
    }

    /**
     * Fetches metadata for the given key, using the R4J Cache decorator.
     *
     * @param key metadata key (e.g. "region", "env", "version")
     * @return metadata value (from cache or downstream)
     */
    public String getMetadata(String key) {
        log.debug("getMetadata: key={}", key);
        try {
            return Cache.decorateCallable(r4jCache, () -> client.fetchMetadata(key)).apply(key);
        } catch (Throwable t) {
            throw new RuntimeException("Cache lookup failed for key=" + key, t);
        }
    }

    /** Returns all well-known metadata keys in one call (batch cache demo). */
    public Map<String, String> getAllMetadata() {
        String[] keys = {"region", "env", "version", "owner", "sla"};
        var result = new java.util.LinkedHashMap<String, String>();
        for (String key : keys) {
            try {
                result.put(key, getMetadata(key));
            } catch (Exception e) {
                result.put(key, "ERROR: " + e.getMessage());
            }
        }
        return result;
    }

    /** Invalidates all cache entries. */
    public void clearCache() {
        jCache.clear();
        cacheConfig.cacheHits.set(0);
        cacheConfig.cacheMisses.set(0);
        log.info("Metadata cache cleared");
    }

    /** Returns current hit/miss counters. */
    public Map<String, Long> getStats() {
        return Map.of(
                "hits",   cacheConfig.cacheHits.get(),
                "misses", cacheConfig.cacheMisses.get()
        );
    }
}
