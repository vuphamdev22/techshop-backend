package com.techshop.backend.dto.request;


import lombok.Data;

@Data
public class UpdateProfileRequest {

    private String firstName;
    private String lastName;
    private String phone;
    private String address;
}
