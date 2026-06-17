package com.techshop.backend.service;

import com.techshop.backend.dto.request.VietQRGenerateRequest;
import com.techshop.backend.dto.response.VietQRResponse;
import com.techshop.backend.entity.Payment;

public interface VietQRService {

    /**
     * Generate VietQR code data from a payment record
     */
    VietQRResponse generateVietQRCode(Payment payment);

    /**
     * Generate VietQR code data from amount/description without payment record
     */
    VietQRResponse generateVietQRCode(VietQRGenerateRequest request);

    /**
     * Generate QR code image as Base64
     */
    String generateQRCodeImage(String qrContent);

    /**
     * Verify VietQR payment
     */
    boolean verifyPaymentStatus(String transactionId);
}
