package com.visasetu.service;

import com.visasetu.domain.entity.Trip;
import com.visasetu.repository.TripRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Year;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class MockGovernmentDecisionService {

    private final TripRepository tripRepository;
    private final PaymentSseService paymentSseService;
    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * Inspect applications in UNDER_REVIEW and auto-adjudicate after simulation delay.
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
            trip.setUpdatedAt(Instant.now());
            tripRepository.save(trip);

            paymentSseService.emitStatusChange(trip.getReferenceId(), "REJECTED",
                    "Application evaluated by consular authorities. Decision: Application Not Approved.");
            log.info("[Government Adjudication Mock] Trip {} REJECTED based on risk profile", trip.getReferenceId());
        } else {
            String etaNo = "ETA-" + Year.now().getValue() + "-" + (100000 + secureRandom.nextInt(900000));
            LocalDate validUntil = LocalDate.now().plusYears(1);
            String qrToken = "ETA:" + trip.getReferenceId() +
                    "|ETA_NO:" + etaNo +
                    "|NAME:" + trip.getApplicantName().toUpperCase() +
                    "|NAT:" + trip.getNationality() +
                    "|STATUS:APPROVED" +
                    "|VALID_UNTIL:" + validUntil;

            trip.setStatus("APPROVED");
            trip.setEtaNumber(etaNo);
            trip.setQrToken(qrToken);
            trip.setUpdatedAt(Instant.now());
            tripRepository.save(trip);

            paymentSseService.emitStatusChange(trip.getReferenceId(), "APPROVED",
                    "Consular clearance granted! Electronic Travel Authorization " + etaNo + " issued.");
            log.info("[Government Adjudication Mock] Trip {} APPROVED with ETA {}", trip.getReferenceId(), etaNo);
        }
    }
}
