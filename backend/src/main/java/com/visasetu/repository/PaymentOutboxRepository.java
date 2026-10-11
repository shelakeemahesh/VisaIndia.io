package com.visasetu.repository;

import com.visasetu.domain.entity.PaymentOutboxMessage;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PaymentOutboxRepository extends JpaRepository<PaymentOutboxMessage, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM PaymentOutboxMessage p WHERE p.status = :status ORDER BY p.createdAt ASC")
    List<PaymentOutboxMessage> findByStatusForUpdate(@Param("status") String status);

    List<PaymentOutboxMessage> findByStatusOrderByCreatedAtAsc(String status);
}
