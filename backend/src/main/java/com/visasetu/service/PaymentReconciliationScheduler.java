package com.visasetu.service;

import com.visasetu.domain.entity.PaymentOrder;
import com.visasetu.repository.PaymentOrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentReconciliationScheduler {

    private final PaymentOrderRepository paymentOrderRepository;
    private final PaymentService paymentService;

    /**
     * Run every 5 minutes to find payments pending > 10 min and repair state.
     */
    @Scheduled(fixedDelay = 300000, initialDelay = 60000)
    public void reconcilePendingPayments() {
        Instant threshold = Instant.now().minusSeconds(600); // 10 minutes ago
        List<PaymentOrder> staleOrders = paymentOrderRepository.findByStatusAndCreatedAtBefore("CREATED", threshold);

        if (staleOrders.isEmpty()) {
            return;
        }

        log.info("[Payment Reconciliation] Found {} stale payment orders pending > 10m. Initiating gateway audit...", staleOrders.size());

        for (PaymentOrder order : staleOrders) {
            try {
                paymentService.forceReconciliation(order.getReferenceId());
                log.info("[Payment Reconciliation] Successfully processed stale order {} (Ref: {})", order.getOrderId(), order.getReferenceId());
            } catch (Exception e) {
                log.warn("[Payment Reconciliation] Error reconciling order {}: {}", order.getOrderId(), e.getMessage());
            }
        }
    }
}
