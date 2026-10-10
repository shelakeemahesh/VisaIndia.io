package com.visasetu.service;

import com.visasetu.domain.entity.PaymentOutboxMessage;
import com.visasetu.repository.PaymentOutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentOutboxPublisherService {

    private final PaymentOutboxRepository paymentOutboxRepository;

    @Autowired(required = false)
    private RabbitTemplate rabbitTemplate;

    private static final int MAX_RETRIES = 5;

    @Scheduled(fixedDelay = 5000, initialDelay = 10000)
    @Transactional
    public void publishPendingMessages() {
        List<PaymentOutboxMessage> pending = paymentOutboxRepository.findByStatusOrderByCreatedAtAsc("PENDING");
        if (pending.isEmpty()) return;

        for (PaymentOutboxMessage msg : pending) {
            try {
                if (rabbitTemplate != null) {
                    rabbitTemplate.convertAndSend("visa.payment.exchange", msg.getEventType(), msg.getPayload());
                    log.info("Published outbox message {} ({}) to RabbitMQ", msg.getEventId(), msg.getEventType());
                } else {
                    log.debug("RabbitMQ not configured or excluded in profile. Marking outbox event {} as PUBLISHED locally.", msg.getEventId());
                }
                msg.setStatus("PUBLISHED");
                msg.setPublishedAt(Instant.now());
            } catch (Exception e) {
                int retries = msg.getRetryCount() != null ? msg.getRetryCount() + 1 : 1;
                msg.setRetryCount(retries);
                log.warn("Failed to publish outbox event {} (Attempt {}/{}): {}", msg.getEventId(), retries, MAX_RETRIES, e.getMessage());

                if (retries >= MAX_RETRIES) {
                    msg.setStatus("DEAD_LETTER");
                    log.error("Outbox event {} exceeded max retries. Moved to DEAD_LETTER.", msg.getEventId());
                }
            }
            paymentOutboxRepository.save(msg);
        }
    }
}
