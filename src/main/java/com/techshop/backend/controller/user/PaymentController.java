package com.techshop.backend.controller.user;

import com.techshop.backend.dto.request.PaymentCreateRequest;
import com.techshop.backend.dto.response.PaymentResponse;
import com.techshop.backend.service.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/create")
    public ResponseEntity<PaymentResponse> createPayment(
            @RequestParam Long orderId,
            @Valid @RequestBody PaymentCreateRequest request) {

        PaymentResponse response = paymentService.createPayment(orderId, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/callback")
    public ResponseEntity<String> handleCallback(
            @RequestParam String transactionId,
            @RequestParam boolean success,
            @RequestParam(required = false) String note) {

        paymentService.handlePaymentCallback(transactionId, success, note);
        return ResponseEntity.ok("Callback processed");
    }

    @GetMapping("/order/{orderId}")
    public ResponseEntity<PaymentResponse> getPaymentByOrderId(@PathVariable Long orderId) {
        PaymentResponse response = paymentService.getPaymentByOrderId(orderId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/vnpay-return")
    public ResponseEntity<String> handleVnPayReturn(HttpServletRequest request) {
        try {
            // Lấy các tham số từ VNPay
            Map<String, String[]> parameterMap = request.getParameterMap();
            Map<String, String> vnpParams = new HashMap<>();

            for (Map.Entry<String, String[]> entry : parameterMap.entrySet()) {
                vnpParams.put(entry.getKey(), entry.getValue()[0]);
            }

            // Xử lý callback từ VNPay
            paymentService.handleVnPayCallback(vnpParams);

            String transactionId = vnpParams.get("vnp_TxnRef");
            String responseCode = vnpParams.get("vnp_ResponseCode");
            boolean success = "00".equals(responseCode);

            log.info("VNPay return processed - Transaction: {}, Success: {}", transactionId, success);

            // Redirect về frontend với kết quả
            String redirectUrl = "http://localhost:5173/payment-result?success=" + success +
                               "&transactionId=" + transactionId +
                               "&responseCode=" + responseCode;

            return ResponseEntity.status(302)
                    .header("Location", redirectUrl)
                    .build();

        } catch (Exception e) {
            log.error("Error handling VNPay return", e);
            return ResponseEntity.status(302)
                    .header("Location", "http://localhost:5173/payment-result?success=false&error=processing_error")
                    .build();
        }
    }
}