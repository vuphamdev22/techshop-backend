package com.techshop.backend.controller.user;

import com.techshop.backend.dto.request.PaymentCreateRequest;
import com.techshop.backend.dto.response.PaymentResponse;
import com.techshop.backend.service.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * Tạo payment và trả về URL VNPAY
     */
    @PostMapping("/orders/{orderId}")
    public ResponseEntity<PaymentResponse> createPayment(
            @PathVariable Long orderId,
            @Valid @RequestBody PaymentCreateRequest request,
            HttpServletRequest httpRequest) {

        // Lấy IP thật của user
        String ipAddress = httpRequest.getRemoteAddr();

        PaymentResponse response = paymentService.createPayment(orderId, request, ipAddress);
        return ResponseEntity.ok(response);
    }

    /**
     * VNPAY redirect về sau khi thanh toán
     */
    @GetMapping("/vnpay-return")
    public void vnpayReturn(@RequestParam Map<String, String> params, HttpServletResponse response) throws IOException {

        boolean isSuccess = paymentService.handleVnpayReturn(params);

        String orderId = paymentService.getOrderIdFromTxnRef(params);

        String redirectUrl;

        if (isSuccess) {
            redirectUrl = "http://localhost:5173/order-success?orderId=" + orderId + "&status=paid";
        } else {
            redirectUrl = "http://localhost:5173/order-failed?orderId=" + orderId + "&status=failed";
        }

        response.sendRedirect(redirectUrl);
    }
    /**
     * Lấy payment theo order
     */
    @GetMapping("/orders/{orderId}")
    public ResponseEntity<PaymentResponse> getPaymentByOrderId(@PathVariable Long orderId) {

        PaymentResponse response = paymentService.getPaymentByOrderId(orderId);
        return ResponseEntity.ok(response);
    }
}