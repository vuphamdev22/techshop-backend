package com.techshop.backend.controller;

import com.techshop.backend.dto.request.VietQRGenerateRequest;
import com.techshop.backend.dto.response.VietQRResponse;
import com.techshop.backend.entity.Payment;
import com.techshop.backend.exception.AppException;
import com.techshop.backend.exception.ErrorCode;
import com.techshop.backend.repository.PaymentRepository;
import com.techshop.backend.service.VietQRService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/vietqr")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
@Validated
public class VietQRController {

    private final VietQRService vietQRService;
    private final PaymentRepository paymentRepository;

    /**
     * Generate VietQR QR Code cho payment
     */
    @PostMapping("/generate/{transactionId}")
    public ResponseEntity<VietQRResponse> generateVietQRCode(@PathVariable String transactionId) {
        log.info("Generating VietQR code for transaction: {}", transactionId);

        try {
            Payment payment = paymentRepository.findByTransactionId(transactionId)
                    .orElseThrow(() -> new AppException(ErrorCode.PAYMENT_NOT_FOUND));

            VietQRResponse response = vietQRService.generateVietQRCode(payment);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error generating VietQR code: ", e);
            throw new AppException(ErrorCode.PAYMENT_CREATION_FAILED);
        }
    }

    /**
     * Generate VietQR QR Code from raw amount/description
     */
    @PostMapping("/generate")
    public ResponseEntity<VietQRResponse> generateVietQRCode(@Valid @RequestBody VietQRGenerateRequest request) {
        log.info("Generating VietQR code from raw request: amount={}, description={}", request.getAmount(), request.getDescription());

        try {
            VietQRResponse response = vietQRService.generateVietQRCode(request);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error generating VietQR code: ", e);
            throw new AppException(ErrorCode.PAYMENT_CREATION_FAILED);
        }
    }

    /**
     * Get VietQR info mà không cần generate lại
     */
    @GetMapping("/info/{transactionId}")
    public ResponseEntity<VietQRResponse> getVietQRInfo(@PathVariable String transactionId) {
        log.info("Getting VietQR info for transaction: {}", transactionId);

        try {
            Payment payment = paymentRepository.findByTransactionId(transactionId)
                    .orElseThrow(() -> new AppException(ErrorCode.PAYMENT_NOT_FOUND));

            VietQRResponse response = vietQRService.generateVietQRCode(payment);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error getting VietQR info: ", e);
            throw new AppException(ErrorCode.PAYMENT_NOT_FOUND);
        }
    }

    /**
     * Verify payment status (polling endpoint)
     * Frontend sẽ poll endpoint này để check xem customer đã thanh toán chưa
     */
    @GetMapping("/verify/{transactionId}")
    public ResponseEntity<Boolean> verifyPayment(@PathVariable String transactionId) {
        log.info("Verifying payment status for transaction: {}", transactionId);

        try {
            Payment payment = paymentRepository.findByTransactionId(transactionId)
                    .orElseThrow(() -> new AppException(ErrorCode.PAYMENT_NOT_FOUND));

            // Check if payment status is SUCCESS
            boolean isVerified = vietQRService.verifyPaymentStatus(transactionId);
            return ResponseEntity.ok(isVerified);

        } catch (Exception e) {
            log.error("Error verifying payment: ", e);
            return ResponseEntity.ok(false);
        }
    }
}
