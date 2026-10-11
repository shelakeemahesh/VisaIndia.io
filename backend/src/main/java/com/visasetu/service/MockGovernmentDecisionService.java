package com.visasetu.service;

import com.visasetu.domain.entity.Trip;
import com.visasetu.repository.TripRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Year;
import java.util.List;

@Service
@Profile({"dev", "local", "test"})
@ConditionalOnProperty(name = "visasetu.mock.adjudication.enabled", havingValue = "true", matchIfMissing = false)
@RequiredArgsConstructor
@Slf4j
public class MockGovernmentDecisionService {

    private final TripRepository tripRepository;
    private final PaymentSseService paymentSseService;
    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * Inspect applications in UNDER_REVIEW and auto-adjudicate after simulation delay.
     * Strictly isolated to dev/local/test profiles with explicit configuration flag enabled.
     */
    @Scheduled(fixedDelay = 10000, initialDelay = 5000)
    @Transactional
    public void adjudicatePendingApplications() {
        List<Trip> underReviewTrips = tripRepository.findByStatusOrderByCreatedAtAsc("UNDER_REVIEW");

        if (underReviewTrips.isEmpty()) {
            return;
        }

        for (Trip trip : underReviewTrips) {
            // Check if application has been under review for at least 8 seconds
            if (trip.getUpdatedAt() != null && Instant.now().isAfter(trip.getUpdatedAt().plusSeconds(8))) {
                adjudicateTrip(trip);
            }
        }
    }

    private void adjudicateTrip(Trip trip) {
        // Business logic rule: Reject if name contains "Rejected" or "Deny"
        boolean isRejected = trip.getApplicantName().toLowerCase().contains("reject")
                || trip.getApplicantName().toLowerCase().contains("deny");

        if (isRejected) {
            trip.setStatus("REJECTED");
            trip.setPreparationStatus("PREPARATION_IN_PROGRESS");
            trip.setUpdatedAt(Instant.now());
            tripRepository.save(trip);

            paymentSseService.emitStatusChange(trip.getReferenceId(), "REJECTED",
                    "Simulated evaluation complete. Risk check flagged application for manual review.");
            log.info("[Government Adjudication Mock - DEV ONLY] Trip {} REJECTED based on risk profile", trip.getReferenceId());
        } else {
            String simEtaNo = "SIM-ETA-" + Year.now().getValue() + "-" + (100000 + secureRandom.nextInt(900000));
            LocalDate validUntil = LocalDate.now().plusYears(1);
            String qrToken = "SIM_TEST_ETA:" + trip.getReferenceId() +
                    "|ETA_NO:" + simEtaNo +
                    "|NAME:" + trip.getApplicantName().toUpperCase() +
                    "|NAT:" + trip.getNationality() +
                    "|STATUS:READY_FOR_OFFICIAL_HANDOFF" +
                    "|VALID_UNTIL:" + validUntil +
                    "|NOTE:SIMULATION_DEV_MODE_ONLY";

            trip.setStatus("APPROVED");
            trip.setPreparationStatus("READY_FOR_OFFICIAL_HANDOFF");
            trip.setEtaNumber(simEtaNo);
            trip.setQrToken(qrToken);
            trip.setUpdatedAt(Instant.now());
            tripRepository.save(trip);

            paymentSseService.emitStatusChange(trip.getReferenceId(), "APPROVED",
                    "Preparation assistance review completed (Simulation Mode). Ready for official government submission: " + simEtaNo);
            log.info("[Government Adjudication Mock - DEV ONLY] Trip {} marked READY_FOR_OFFICIAL_HANDOFF with {}", trip.getReferenceId(), simEtaNo);
        }
    }
}
