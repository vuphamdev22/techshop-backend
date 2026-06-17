package com.techshop.backend.controller.user;

import com.techshop.backend.dto.request.ChangePasswordRequest;
import com.techshop.backend.dto.request.UpdateProfileRequest;
import com.techshop.backend.entity.User;
import com.techshop.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @GetMapping("/me")
    public User getCurrentUser(Authentication authentication){

        String email = authentication.getName();

        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    @PutMapping("/me")
    public User updateProfile(Authentication authentication,
                              @RequestBody UpdateProfileRequest request){

        String email = authentication.getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow();

        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setPhone(request.getPhone());
        user.setAddress(request.getAddress());
        user.setBirthday(request.getBirthday());
        user.setGender(request.getGender());
        if (request.getEmailSubscribed() != null) {
            user.setEmailSubscribed(request.getEmailSubscribed());
        }

        return userRepository.save(user);
    }

    @PutMapping("/change-password")
    public String changePassword(Authentication authentication,
                                 @RequestBody ChangePasswordRequest request){

        String email = authentication.getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow();

        if(!passwordEncoder.matches(request.getOldPassword(), user.getPassword())){
            throw new RuntimeException("Old password incorrect");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        return "Password updated";
    }
}
