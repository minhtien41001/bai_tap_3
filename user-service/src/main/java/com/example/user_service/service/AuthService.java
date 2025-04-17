package com.example.user_service.service;

import com.example.user_service.config.JwtUtil;
import com.example.user_service.dto.*;
import com.example.user_service.entity.Role;
import com.example.user_service.entity.User;
import com.example.user_service.event.KafkaProducer;
import com.example.user_service.repository.UserRepository;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;
    private final KafkaProducer kafkaProducer;

    public AuthService(UserRepository userRepository, JwtUtil jwtUtil, PasswordEncoder passwordEncoder, KafkaProducer kafkaProducer) {
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
        this.passwordEncoder = passwordEncoder;
        this.kafkaProducer = kafkaProducer;
    }

    public User registerUser(String username, String password, String email, Role role) {
        Role userRole;
        userRole = Objects.requireNonNullElse(role, Role.EMPLOYEE);

        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));  // Mã hóa mật khẩu
        user.setEmail(email);
        user.setRole(userRole);  // Gán vai trò cho người dùng
        User savedUser = userRepository.save(user);

        kafkaProducer.sendUserRegisteredEvent(new UserRegisteredEvent(email));

        return savedUser;
    }

    public AuthResponse login(LoginRequest loginRequest) {
        User user = userRepository.findByUsername(loginRequest.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("User not found!"));

        if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Invalid credentials!");
        }

        String token = jwtUtil.generateToken(user.getUsername(), user.getRole());

        return new AuthResponse(token);
    }

    public void updateCurrentUser(String username, UpdateProfileRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            user.setEmail(request.getEmail());
        }

        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        userRepository.save(user);
    }
}