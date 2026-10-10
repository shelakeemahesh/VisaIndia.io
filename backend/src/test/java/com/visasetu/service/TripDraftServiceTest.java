package com.visasetu.service;

import com.visasetu.domain.entity.TripDraft;
import com.visasetu.repository.TripDraftRepository;
import jakarta.persistence.OptimisticLockException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TripDraftServiceTest {

    @Mock
    private TripDraftRepository tripDraftRepository;

    @InjectMocks
    private TripDraftService tripDraftService;

    @Test
    @DisplayName("createOrGetDraft returns existing draft if found")
    void createOrGetDraft_existingDraft_returnsExisting() {
        String email = "traveler@example.com";
        TripDraft existing = TripDraft.builder()
                .draftId(UUID.randomUUID())
                .referenceId("VS-2026-9999")
                .applicantEmail(email)
                .version(1L)
                .build();

        given(tripDraftRepository.findByApplicantEmailIgnoreCase(email))
                .willReturn(Optional.of(existing));

        TripDraft result = tripDraftService.createOrGetDraft(email, "USA", "Tourist");

        assertThat(result.getReferenceId()).isEqualTo("VS-2026-9999");
    }

    @Test
    @DisplayName("autosaveDraft succeeds and updates version when expectedVersion matches")
    void autosaveDraft_versionMatches_savesSuccessfully() {
        UUID draftId = UUID.randomUUID();
        TripDraft existing = TripDraft.builder()
                .draftId(draftId)
                .referenceId("VS-2026-1234")
                .applicantEmail("traveler@example.com")
                .sharedProfileJson("{\"name\":\"Alice\"}")
                .modulesJson("{}")
                .version(2L)
                .currentStep(1)
                .build();

        given(tripDraftRepository.findById(draftId)).willReturn(Optional.of(existing));
        given(tripDraftRepository.save(any(TripDraft.class))).willAnswer(inv -> {
            TripDraft d = inv.getArgument(0);
            d.setVersion(d.getVersion() + 1);
            return d;
        });

        TripDraft saved = tripDraftService.autosaveDraft(
                draftId,
                2L,
                "{\"name\":\"Alice Updated\"}",
                "{\"eVisa\":{}}",
                2
        );

        assertThat(saved.getSharedProfileJson()).contains("Alice Updated");
        assertThat(saved.getCurrentStep()).isEqualTo(2);
        assertThat(saved.getVersion()).isEqualTo(3L);
        verify(tripDraftRepository).save(existing);
    }

    @Test
    @DisplayName("autosaveDraft throws OptimisticLockException when expectedVersion conflicts")
    void autosaveDraft_versionConflict_throwsOptimisticLockException() {
        UUID draftId = UUID.randomUUID();
        TripDraft existing = TripDraft.builder()
                .draftId(draftId)
                .referenceId("VS-2026-1234")
                .version(3L)
                .build();

        given(tripDraftRepository.findById(draftId)).willReturn(Optional.of(existing));

        assertThatThrownBy(() -> tripDraftService.autosaveDraft(
                draftId,
                1L, // stale client version
                "{}",
                "{}",
                1
        )).isInstanceOf(OptimisticLockException.class)
          .hasMessageContaining("Concurrent edit detected");
    }
}
