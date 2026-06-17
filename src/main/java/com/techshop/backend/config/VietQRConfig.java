package com.techshop.backend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "vietqr")
@Data
public class VietQRConfig {

    private String accountNumber;      // Số tài khoản: 0344998620
    private String accountName;        // Tên tài khoản: PHAM VAN VU
    private String bankCode;           // Mã ngân hàng: MB (MB Bank)
    private String bankName;           // Tên ngân hàng: MB BANK
    private String template;           // Template: compact hoặc standard
    private Integer qrCodeSize;        // Kích thước QR (pixels)
}
