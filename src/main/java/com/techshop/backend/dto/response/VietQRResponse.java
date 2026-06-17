package com.techshop.backend.dto.response;

import lombok.Data;

@Data
public class VietQRResponse {

    private String accountNumber;    // 0344998620
    private String accountName;      // PHAM VAN VU
    private String bankCode;         // MB
    private String bankName;         // MB BANK
    private Double amount;
    private String description;
    private String qrCodeImage;      // Base64 encoded QR image
    private String transactionId;
}
