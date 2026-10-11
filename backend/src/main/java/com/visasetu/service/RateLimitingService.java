package com.visasetu.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe sliding window rate limiter with bounded memory eviction.
 */
@Service
@Slf4j
public class RateLimitingService {

    private static final int MAX_TRACKED_KEYS = 10_000;
    private final Map<String, Deque<Long>> requestLogs = new ConcurrentHashMap<>();

    /**
     * Attempts to acquire an execution permit within the time window.
     *
     * @param key           identifier (e.g., action:ip or action:email)
     * @param maxRequests   maximum allowed requests within window
     * @param windowSeconds length of sliding window in seconds
     * @return true if permitted, false if rate limit exceeded
     */
    public synchronized boolean tryAcquire(String key, int maxRequests, long windowSeconds) {
        long now = Instant.now().toEpochMilli();
        long windowStart = now - (windowSeconds * 1000L);

        // Memory bounding: if map has grown too large, perform batch eviction of stale keys
        if (requestLogs.size() > MAX_TRACKED_KEYS) {
            pruneStaleKeys(windowStart);
        }

        Deque<Long> timestamps = requestLogs.computeIfAbsent(key, k -> new ArrayDeque<>());

        // Purge expired entries
        while (!timestamps.isEmpty() && timestamps.peekFirst() < windowStart) {
            timestamps.pollFirst();
        }

        if (timestamps.size() >= maxRequests) {
            log.warn("Rate limit exceeded for key: {} (Limit: {} in {}s)", key, maxRequests, windowSeconds);
            return false;
        }

        timestamps.addLast(now);
        return true;
    }

    private void pruneStaleKeys(long thresholdEpochMilli) {
        requestLogs.entrySet().removeIf(entry -> {
            Deque<Long> queue = entry.getValue();
            synchronized (queue) {
                while (!queue.isEmpty() && queue.peekFirst() < thresholdEpochMilli) {
                    queue.pollFirst();
                }
                return queue.isEmpty();
            }
        });
    }

    public synchronized void reset(String key) {
        requestLogs.remove(key);
    }

    public int getTrackedKeyCount() {
        return requestLogs.size();
    }
}
