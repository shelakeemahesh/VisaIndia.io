package com.visasetu.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe in-memory sliding window rate limiter for security-sensitive operations.
 */
@Service
@Slf4j
public class RateLimitingService {

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

    public synchronized void reset(String key) {
        requestLogs.remove(key);
    }
}
