package com.example.user_service.dto;

import com.example.user_service.entity.Role;
import lombok.Data;

@Data
public class UserRegisterRequest {
    private String username;
    private String password;
    private String email;
    private Role role;
}
