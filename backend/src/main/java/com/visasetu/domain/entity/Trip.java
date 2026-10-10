package com.visasetu.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "trips")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Trip {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "reference_id", nullable = false, unique = true, length = 50)
    private String referenceId;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "applicant_email", nullable = false, length = 150)
    private String applicantEmail;

    @Column(name = "applicant_name", nullable = false)
    private String applicantName;

    @Column(name = "passport_number_encrypted", columnDefinition = "TEXT")
    private String passportNumberEncrypted;

    @Column(name = "date_of_birth_encrypted", columnDefinition = "TEXT")
    private String dateOfBirthEncrypted;

    @Column(nullable = false, length = 100)
    private String nationality;

    @Column(name = "category_name", nullable = false)
    private String categoryName;

    @Column(nullable = false, length = 50)
    @Builder.Default
    private String status = "DRAFT"; // DRAFT, SUBMITTED, PAYMENT_PENDING, PAID, UNDER_REVIEW, APPROVED, REJECTED, PAYMENT_FAILED

    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(nullable = false, length = 10)
    @Builder.Default
    private String currency = "USD";

    @Column(name = "arrival_date")
    private LocalDate arrivalDate;

    @Column(name = "port_of_arrival")
    private String portOfArrival;

    @Column(name = "eta_number")
    private String etaNumber;

    @Column(name = "qr_token", columnDefinition = "TEXT")
    private String qrToken;

    @Version
    @Column(nullable = false)
    @Builder.Default
    private Long version = 0L;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private Instant updatedAt = Instant.now();

    @Column(name = "submitted_at")
    private Instant submittedAt;
}
