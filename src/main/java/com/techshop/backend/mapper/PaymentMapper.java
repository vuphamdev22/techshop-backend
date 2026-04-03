package com.techshop.backend.mapper;

import com.techshop.backend.dto.response.PaymentResponse;
import com.techshop.backend.entity.Payment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PaymentMapper {

    @Mapping(target = "orderId", source = "order.id")
    PaymentResponse toResponse(Payment payment);

    default PaymentResponse toResponseWithUrl(Payment payment, String paymentUrl) {
        PaymentResponse response = toResponse(payment);
        response.setPaymentUrl(paymentUrl);
        return response;
    }
}