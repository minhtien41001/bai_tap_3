package com.example.user_service.service;

import com.example.user_service.dto.*;
import com.example.user_service.entity.Role;
import com.example.user_service.entity.User;
import com.example.user_service.event.KafkaProducer;
import com.example.user_service.repository.UserRepository;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final KafkaProducer kafkaProducer;
    private final KeycloakAdminClientService keycloakAdminClientService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, KafkaProducer kafkaProducer, KeycloakAdminClientService keycloakAdminClientService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.kafkaProducer = kafkaProducer;
        this.keycloakAdminClientService = keycloakAdminClientService;
    }

    /**
     * Chức năng đăng ký user.
     *
     * @param username tên đăng nhập
     * @param password mật khẩu
     * @param email    email
     * @param role    role
     */
    public void registerUser(String username, String password, String email, Role role) {
        // Nếu không truyền role → mặc định EMPLOYEE
        Role actualRole = (role != null) ? role : Role.EMPLOYEE;

        // 1. Tạo user trên Keycloak và gán role
        keycloakAdminClientService.createUser(username, email, password, actualRole.name());

        // 2. Lưu user vào DB local
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setEmail(email);
        user.setRole(actualRole); // nếu bạn lưu cả role ở DB local

        User savedUser = userRepository.save(user);

        kafkaProducer.sendUserRegisteredEvent(new UserRegisteredEvent(email));

    }


    /**
     * Cập nhật thông tin user hiện tại.
     * @param username tên đăng nhập
     * @param request request chứa thông tin cập nhật
     */
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