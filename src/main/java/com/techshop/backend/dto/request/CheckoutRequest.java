package com.techshop.backend.dto.request;

import com.techshop.backend.enums.PaymentMethod;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class CheckoutRequest {

    @NotBlank(message = "First name is required")
    private String firstName;

    @NotBlank(message = "Last name is required")
    private String lastName;

    @Email(message = "Invalid email")
    @NotBlank(message = "Email is required")
    private String email;

    @NotBlank(message = "Phone is required")
    private String phone;

    @NotBlank(message = "Address is required")
    private String address;

    @NotBlank(message = "City is required")
    private String city;

    @NotBlank(message = "State is required")
    private String state;

    @NotBlank(message = "Zip code is required")
    private String zipCode;

    // 🔥 đổi từ String -> Enum
    @NotNull(message = "Payment method is required")
    private PaymentMethod paymentMethod;
}