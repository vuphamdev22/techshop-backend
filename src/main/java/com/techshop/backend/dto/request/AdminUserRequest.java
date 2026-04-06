package com.techshop.backend.dto.request;

import com.techshop.backend.enums.Role;
import lombok.Data;

@Data
public class AdminUserRequest {
    private String firstName;
    private String lastName;
    private String phone;
    private String address;
    private Role role;
    private Boolean enabled;
}
