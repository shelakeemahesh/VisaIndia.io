package com.visasetu.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "demo_applications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DemoApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "reference_id", nullable = false, unique = true, length = 50)
    private String referenceId;

    @Column(name = "applicant_email", nullable = false)
    private String applicantEmail;

    @Column(name = "applicant_name", nullable = false)
    private String applicantName;

    @Column(nullable = false, length = 100)
    private String nationality;

    @Column(name = "category_name", nullable = false, length = 150)
    private String categoryName;

    @Column(nullable = false, length = 50)
    private String status; // DRAFT, SUBMITTED, PAYMENT_PENDING, UNDER_REVIEW, APPROVED, REJECTED, PAYMENT_FAILED

    @Column(name = "order_id", length = 100)
    private String orderId;

    @Column(name = "payment_id", length = 100)
    private String paymentId;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(name = "arrival_date")
    private LocalDate arrivalDate;

    @Column(name = "port_of_arrival", length = 100)
    private String portOfArrival;

    @Column(name = "qr_data", columnDefinition = "TEXT")
    private String qrData;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private Instant updatedAt = Instant.now();
}
