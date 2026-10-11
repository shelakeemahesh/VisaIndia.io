package com.visasetu.service;

import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import org.json.JSONObject;
import com.visasetu.domain.entity.PaymentEvent;
import com.visasetu.domain.entity.PaymentOrder;
import com.visasetu.domain.entity.PaymentOutboxMessage;
import com.visasetu.domain.entity.Trip;
import com.visasetu.dto.PaymentVerificationDto;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

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

    @Mock
    private RazorpayClient razorpayClient;

    @InjectMocks
    private PaymentService paymentService;

    private final String webhookSecret = "TestSecretKeyForTestingWebhookSignatures2026";
    private final String razorpayKeySecret = "TestRazorpayKeySecretForClientVerification123";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(paymentService, "webhookSecret", webhookSecret);
        ReflectionTestUtils.setField(paymentService, "razorpayKeyId", "rzp_test_mock");
        ReflectionTestUtils.setField(paymentService, "razorpayKeySecret", razorpayKeySecret);
        paymentService.setRazorpayClient(razorpayClient);
    }

    private String calculateSignature(String payload, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
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
        assertThat(result.get("amountSubunits")).isEqualTo(2500L);
    }

    @Test
    @DisplayName("createPaymentOrder creates real gateway order in minor units with receipt = referenceId")
    void createPaymentOrder_callsGatewayWithMinorUnits() throws Exception {
        String refId = "VS-2026-8888";
        String key = "idem-key-new";
        Trip trip = Trip.builder()
                .id(UUID.randomUUID())
                .referenceId(refId)
                .applicantEmail("traveler@example.com")
                .totalAmount(new BigDecimal("40.00"))
                .currency("USD")
                .status("DRAFT")
                .build();

        com.razorpay.Order mockRzpOrder = mock(com.razorpay.Order.class);
        given(mockRzpOrder.get("id")).willReturn("order_live_rzp_999");
        com.razorpay.OrderClient mockOrderClient = mock(com.razorpay.OrderClient.class);
        razorpayClient.orders = mockOrderClient;
        given(mockOrderClient.create(any(JSONObject.class))).willReturn(mockRzpOrder);

        given(paymentOrderRepository.findByIdempotencyKey(key)).willReturn(Optional.empty());
        given(tripRepository.findByReferenceIdIgnoreCase(refId)).willReturn(Optional.of(trip));

        Map<String, Object> result = paymentService.createPaymentOrder(refId, key);

        assertThat(result.get("orderId")).isEqualTo("order_live_rzp_999");
        assertThat(result.get("amountSubunits")).isEqualTo(4000L);
        assertThat(result.get("referenceId")).isEqualTo(refId);
        verify(paymentOrderRepository).save(any(PaymentOrder.class));
        assertThat(trip.getStatus()).isEqualTo("PAYMENT_PENDING");
    }

    @Test
    @DisplayName("verifyPayment succeeds with valid HMAC signature and advances trip to UNDER_REVIEW")
    void verifyPayment_validSignature_settlesPayment() throws Exception {
        String orderId = "order_test_rzp_1";
        String paymentId = "pay_test_rzp_1";
        String signature = calculateSignature(orderId + "|" + paymentId, razorpayKeySecret);

        UUID tripId = UUID.randomUUID();
        PaymentOrder order = PaymentOrder.builder()
                .id(UUID.randomUUID())
                .tripId(tripId)
                .orderId(orderId)
                .referenceId("VS-2026-7777")
                .amount(new BigDecimal("25.00"))
                .currency("USD")
                .status("CREATED")
                .build();

        Trip trip = Trip.builder()
                .id(tripId)
                .referenceId("VS-2026-7777")
                .status("PAYMENT_PENDING")
                .build();

        given(paymentEventRepository.existsById("verify_" + paymentId)).willReturn(false);
        given(paymentEventRepository.existsByPaymentId(paymentId)).willReturn(false);
        given(paymentOrderRepository.findByOrderId(orderId)).willReturn(Optional.of(order));
        given(tripRepository.findById(tripId)).willReturn(Optional.of(trip));

        PaymentVerificationDto dto = PaymentVerificationDto.builder()
                .referenceId("VS-2026-7777")
                .razorpayOrderId(orderId)
                .razorpayPaymentId(paymentId)
                .razorpaySignature(signature)
                .build();

        Map<String, Object> result = paymentService.verifyPayment(dto);

        assertThat(result.get("status")).isEqualTo("VERIFIED");
        assertThat(order.getStatus()).isEqualTo("PAID");
        assertThat(trip.getStatus()).isEqualTo("UNDER_REVIEW");
        verify(paymentOutboxRepository).save(any(PaymentOutboxMessage.class));
        verify(paymentEventRepository).save(any(PaymentEvent.class));
    }

    @Test
    @DisplayName("verifyPayment rejects tampered signature")
    void verifyPayment_invalidSignature_throwsException() {
        PaymentVerificationDto dto = PaymentVerificationDto.builder()
                .referenceId("VS-2026-7777")
                .razorpayOrderId("order_test_1")
                .razorpayPaymentId("pay_test_1")
                .razorpaySignature("tampered_signature_hex")
                .build();

        assertThatThrownBy(() -> paymentService.verifyPayment(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid cryptographic signature");
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
        String signature = calculateSignature(payload, webhookSecret);

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

    @Test
    @DisplayName("processWebhook never downgrades PAID order or UNDER_REVIEW trip on payment.failed")
    void processWebhook_paymentFailed_whenAlreadyPaid_doesNotDowngrade() throws Exception {
        String payload = "{\"event\":\"payment.failed\",\"order_id\":\"order_test_1\",\"event_id\":\"evt_fail_1\"}";
        String signature = calculateSignature(payload, webhookSecret);

        UUID tripId = UUID.randomUUID();
        PaymentOrder order = PaymentOrder.builder()
                .id(UUID.randomUUID())
                .tripId(tripId)
                .orderId("order_test_1")
                .referenceId("VS-2026-5555")
                .amount(new BigDecimal("25.00"))
                .currency("USD")
                .status("PAID")
                .build();

        Trip trip = Trip.builder()
                .id(tripId)
                .referenceId("VS-2026-5555")
                .status("UNDER_REVIEW")
                .build();

        given(paymentOrderRepository.findByOrderId("order_test_1")).willReturn(Optional.of(order));
        given(tripRepository.findById(tripId)).willReturn(Optional.of(trip));

        boolean result = paymentService.processWebhook(payload, signature, "evt_fail_1");

        assertThat(result).isTrue();
        // Preserves PAID and UNDER_REVIEW without downgrade
        assertThat(order.getStatus()).isEqualTo("PAID");
        assertThat(trip.getStatus()).isEqualTo("UNDER_REVIEW");
        verify(paymentOrderRepository, never()).save(any(PaymentOrder.class));
    }

    @Test
    @DisplayName("processRefund is idempotent and records refundId")
    void processRefund_idempotent() {
        UUID tripId = UUID.randomUUID();
        PaymentOrder order = PaymentOrder.builder()
                .id(UUID.randomUUID())
                .tripId(tripId)
                .orderId("order_test_refund")
                .referenceId("VS-2026-3333")
                .paymentId("pay_123")
                .status("PAID")
                .build();

        Trip trip = Trip.builder()
                .id(tripId)
                .referenceId("VS-2026-3333")
                .status("UNDER_REVIEW")
                .build();

        given(paymentOrderRepository.findByOrderId("order_test_refund")).willReturn(Optional.of(order));
        given(tripRepository.findById(tripId)).willReturn(Optional.of(trip));

        Map<String, Object> firstRefund = paymentService.processRefund("order_test_refund", "rfnd_manual_1", BigDecimal.ZERO, "User requested");
        assertThat(firstRefund.get("status")).isEqualTo("REFUNDED");
        assertThat(order.getRefundId()).isEqualTo("rfnd_manual_1");
        assertThat(order.getStatus()).isEqualTo("REFUNDED");

        // Second call with same order
        Map<String, Object> secondRefund = paymentService.processRefund("order_test_refund", "rfnd_manual_2", BigDecimal.ZERO, "Duplicate request");
        assertThat(secondRefund.get("status")).isEqualTo("ALREADY_REFUNDED");
    }
}
