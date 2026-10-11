package com.visasetu.security;

import com.visasetu.domain.entity.Trip;
import com.visasetu.domain.entity.TripDraft;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.UUID;

/**
 * Reusable zero-trust ownership validation policy.
 * Null owner = deny.
 */
@Component
@Slf4j
public class OwnershipPolicy {

    /**
     * Asserts that currentUserId is not null, ownerId is not null, and they match.
     * Otherwise throws AccessDeniedException.
     */
    public void assertOwner(UUID currentUserId, UUID ownerId) {
        if (currentUserId == null || ownerId == null || !Objects.equals(currentUserId, ownerId)) {
            log.warn("OwnershipPolicy assertion failed: caller={}, owner={}", currentUserId, ownerId);
            throw new AccessDeniedException("Access denied: You do not have ownership privileges on this resource.");
        }
    }

    public void assertOwner(UUID currentUserId, Trip trip) {
        if (trip == null) {
            throw new IllegalArgumentException("Trip cannot be null");
        }
        if (SecurityUtils.hasRole("ADMIN")) {
            return;
        }
        assertOwner(currentUserId, trip.getUserId());
    }

    public void assertOwner(UUID currentUserId, TripDraft draft) {
        if (draft == null) {
            throw new IllegalArgumentException("TripDraft cannot be null");
        }
        if (SecurityUtils.hasRole("ADMIN")) {
            return;
        }
        assertOwner(currentUserId, draft.getUserId());
    }

    public boolean isOwner(UUID currentUserId, UUID ownerId) {
        return currentUserId != null && ownerId != null && Objects.equals(currentUserId, ownerId);
    }
}
