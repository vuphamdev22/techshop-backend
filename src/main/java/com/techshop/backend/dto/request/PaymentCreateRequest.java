package com.techshop.backend.dto.request;

import com.techshop.backend.enums.PaymentMethod;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PaymentCreateRequest {

    @NotNull(message = "Payment method is required")
    private PaymentMethod method;

    // Có thể thêm các trường khác nếu cần, như bank code, etc.
}