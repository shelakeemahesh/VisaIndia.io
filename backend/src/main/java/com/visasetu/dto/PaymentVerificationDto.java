package com.visasetu.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentVerificationDto {

    @NotBlank(message = "referenceId is required")
    @JsonAlias({"reference_id", "referenceId"})
    private String referenceId;

    @NotBlank(message = "razorpayOrderId is required")
    @JsonAlias({"razorpay_order_id", "razorpayOrderId"})
    private String razorpayOrderId;

    @NotBlank(message = "razorpayPaymentId is required")
    @JsonAlias({"razorpay_payment_id", "razorpayPaymentId"})
    private String razorpayPaymentId;

    @NotBlank(message = "razorpaySignature is required")
    @JsonAlias({"razorpay_signature", "razorpaySignature"})
    private String razorpaySignature;
}
