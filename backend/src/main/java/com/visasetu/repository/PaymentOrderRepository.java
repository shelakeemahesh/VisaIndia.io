package com.visasetu.repository;

import com.visasetu.domain.entity.PaymentOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentOrderRepository extends JpaRepository<PaymentOrder, UUID> {
    Optional<PaymentOrder> findByIdempotencyKey(String idempotencyKey);
    Optional<PaymentOrder> findByOrderId(String orderId);
    Optional<PaymentOrder> findByReferenceId(String referenceId);
    List<PaymentOrder> findByStatusAndCreatedAtBefore(String status, Instant threshold);
}
