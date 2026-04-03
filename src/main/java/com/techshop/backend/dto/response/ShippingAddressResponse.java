package com.techshop.backend.dto.response;


import lombok.Data;

@Data
public class ShippingAddressResponse {

    private String firstName;
    private String lastName;
    private String email;
    private String phone;

    private String address;
    private String city;
    private String state;
    private String zipCode;
}