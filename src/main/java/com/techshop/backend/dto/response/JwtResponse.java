package com.techshop.backend.dto.response;

import com.techshop.backend.dto.response.UserDTO;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class JwtResponse {
    private String accessToken;
    private String refreshToken;
    private UserDTO user;
}