package com.visasetu.service;

import com.visasetu.domain.entity.PaymentEvent;
import com.visasetu.domain.entity.PaymentOrder;
import com.visasetu.domain.entity.PaymentOutboxMessage;
import com.visasetu.domain.entity.Trip;
import com.visasetu.repository.PaymentEventRepository;
import com.visasetu.repository.PaymentOrderRepository;
import com.visasetu.repository.PaymentOutboxRepository;
import com.visasetu.repository.TripRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentOrderRepository paymentOrderRepository;

    @Mock
    private PaymentOutboxRepository paymentOutboxRepository;

    @Mock
    private PaymentEventRepository paymentEventRepository;

    @Mock
    private TripRepository tripRepository;

    @Mock
    private PaymentSseService paymentSseService;

    @InjectMocks
    private PaymentService paymentService;

    private final String webhookSecret = "TestSecretKeyForTestingWebhookSignatures2026";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(paymentService, "webhookSecret", webhookSecret);
        ReflectionTestUtils.setField(paymentService, "razorpayKeyId", "rzp_test_mock");
    }

    private String calculateSignature(String payload) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKey = new SecretKeySpec(webhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        mac.init(secretKey);
        byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(hash);
    }

    @Test
    @DisplayName("createPaymentOrder is idempotent and returns existing order if idempotency key matches")
    void createPaymentOrder_idempotentKeyMatch_returnsExisting() {
        String key = "idem-key-12345";
        PaymentOrder existing = PaymentOrder.builder()
                .id(UUID.randomUUID())
                .orderId("order_123")
                .referenceId("VS-2026-1001")
                .amount(new BigDecimal("25.00"))
                .currency("USD")
                .status("CREATED")
                .idempotencyKey(key)
                .build();

        given(paymentOrderRepository.findByIdempotencyKey(key)).willReturn(Optional.of(existing));

        Map<String, Object> result = paymentService.createPaymentOrder("VS-2026-1001", key);

        assertThat(result.get("orderId")).isEqualTo("order_123");
        assertThat(result.get("amount")).isEqualTo(new BigDecimal("25.00"));
    }

    @Test
    @DisplayName("processWebhook rejects invalid signatures")
    void processWebhook_invalidSignature_returnsFalse() {
        String payload = "{\"event\":\"payment.captured\"}";
        boolean success = paymentService.processWebhook(payload, "invalid_sig", "evt_1");
        assertThat(success).isFalse();
    }

    @Test
    @DisplayName("processWebhook advances state machine and saves outbox on valid payment.captured")
    void processWebhook_validSignature_advancesStateMachine() throws Exception {
        String payload = "{\"event\":\"payment.captured\",\"order_id\":\"order_test_1\",\"event_id\":\"evt_9999\"}";
        String signature = calculateSignature(payload);

        UUID tripId = UUID.randomUUID();
        PaymentOrder order = PaymentOrder.builder()
                .id(UUID.randomUUID())
                .tripId(tripId)
                .orderId("order_test_1")
                .referenceId("VS-2026-5555")
                .amount(new BigDecimal("25.00"))
                .currency("USD")
                .status("CREATED")
                .build();

        Trip trip = Trip.builder()
                .id(tripId)
                .referenceId("VS-2026-5555")
                .status("PAYMENT_PENDING")
                .build();

        given(paymentEventRepository.existsById("evt_9999")).willReturn(false);
        given(paymentOrderRepository.findByOrderId("order_test_1")).willReturn(Optional.of(order));
        given(tripRepository.findById(tripId)).willReturn(Optional.of(trip));

        boolean result = paymentService.processWebhook(payload, signature, "evt_9999");

        assertThat(result).isTrue();
        assertThat(order.getStatus()).isEqualTo("PAID");
        assertThat(trip.getStatus()).isEqualTo("UNDER_REVIEW");
        verify(paymentOutboxRepository).save(any(PaymentOutboxMessage.class));
        verify(paymentEventRepository).save(any(PaymentEvent.class));
    }
}
