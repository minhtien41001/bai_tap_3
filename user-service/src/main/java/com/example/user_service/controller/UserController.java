package com.example.user_service.controller;

import com.example.user_service.dto.*;
import com.example.user_service.entity.User;
import com.example.user_service.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.web.bind.annotation.*;


import java.security.Principal;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final AuthService authService;

    public UserController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody UserRegisterRequest request) {
        authService.registerUser(request.getUsername(), request.getPassword(), request.getEmail(), request.getRole());
        return ResponseEntity.ok("Đăng ký tài khoản thành công!");
    }


    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(OAuth2AuthenticationToken auth) {
        Map<String, Object> attributes = auth.getPrincipal().getAttributes();

        // Trả về thông tin người dùng từ token
        return ResponseEntity.ok(attributes);
    }

    @PutMapping("/me")
    public ResponseEntity<?> updateMyAccount(@RequestBody UpdateProfileRequest request, Principal principal) {
        String username = principal.getName(); // Lấy từ token của Keycloak

        authService.updateCurrentUser(username, request);

        return ResponseEntity.ok("Cập nhật tài khoản thành công");
    }
}