package com.visasetu.security;

import com.visasetu.domain.entity.Trip;
import com.visasetu.domain.entity.TripDraft;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OwnershipPolicyTest {

    private OwnershipPolicy ownershipPolicy;

    @BeforeEach
    void setUp() {
        ownershipPolicy = new OwnershipPolicy();
    }

    @Test
    @DisplayName("assertOwner succeeds when currentUserId equals ownerId")
    void assertOwner_matchingUser_succeeds() {
        UUID userId = UUID.randomUUID();
        assertThatCode(() -> ownershipPolicy.assertOwner(userId, userId))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("assertOwner throws AccessDeniedException when caller is null")
    void assertOwner_nullCaller_throwsAccessDenied() {
        UUID ownerId = UUID.randomUUID();
        assertThatThrownBy(() -> ownershipPolicy.assertOwner(null, ownerId))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("assertOwner throws AccessDeniedException when owner is null")
    void assertOwner_nullOwner_throwsAccessDenied() {
        UUID callerId = UUID.randomUUID();
        assertThatThrownBy(() -> ownershipPolicy.assertOwner(callerId, (UUID) null))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("assertOwner throws AccessDeniedException when caller does not match owner")
    void assertOwner_mismatchedIds_throwsAccessDenied() {
        UUID callerId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        assertThatThrownBy(() -> ownershipPolicy.assertOwner(callerId, ownerId))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("assertOwner for Trip succeeds for matching user and fails for non-matching")
    void assertOwner_trip_enforcesOwnership() {
        UUID ownerId = UUID.randomUUID();
        UUID callerId = UUID.randomUUID();
        Trip trip = Trip.builder().userId(ownerId).referenceId("VS-2026-TEST").build();

        assertThatCode(() -> ownershipPolicy.assertOwner(ownerId, trip))
                .doesNotThrowAnyException();

        assertThatThrownBy(() -> ownershipPolicy.assertOwner(callerId, trip))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("assertOwner for TripDraft succeeds for matching user and fails for non-matching")
    void assertOwner_tripDraft_enforcesOwnership() {
        UUID ownerId = UUID.randomUUID();
        UUID callerId = UUID.randomUUID();
        TripDraft draft = TripDraft.builder().userId(ownerId).referenceId("VS-2026-TEST").build();

        assertThatCode(() -> ownershipPolicy.assertOwner(ownerId, draft))
                .doesNotThrowAnyException();

        assertThatThrownBy(() -> ownershipPolicy.assertOwner(callerId, draft))
                .isInstanceOf(AccessDeniedException.class);
    }
}
