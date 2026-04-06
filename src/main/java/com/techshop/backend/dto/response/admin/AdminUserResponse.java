package com.techshop.backend.dto.response.admin;

import com.techshop.backend.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserResponse {
    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String address;
    private Role role;
    private boolean enabled;
    private LocalDateTime createdAt;
    private int orderCount;
    private double totalSpent;
}
