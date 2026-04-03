package com.techshop.backend.entity;

import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Embeddable
public class ShippingAddress {

    private String firstName;
    private String lastName;
    private String email;
    private String phone;

    private String address;
    private String city;
    private String state;
    private String zipCode;
}