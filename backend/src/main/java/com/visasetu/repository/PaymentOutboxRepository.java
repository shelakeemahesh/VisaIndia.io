package com.visasetu.repository;

import com.visasetu.domain.entity.PaymentOutboxMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PaymentOutboxRepository extends JpaRepository<PaymentOutboxMessage, UUID> {
    List<PaymentOutboxMessage> findByStatusOrderByCreatedAtAsc(String status);
}
