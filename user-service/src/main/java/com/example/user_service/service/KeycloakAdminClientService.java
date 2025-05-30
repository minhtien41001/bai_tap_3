package com.example.user_service.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class KeycloakAdminClientService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${keycloak.base-url}")
    private String baseUrl;

    @Value("${keycloak.realm-book}")
    private String realm;

    @Value("${keycloak.client-id}")
    private String clientId;

    @Value("${keycloak.client-secret}")
    private String clientSecret;

    @Value("${keycloak.admin-username}")
    private String adminUsername;

    @Value("${keycloak.admin-password}")
    private String adminPassword;

    public KeycloakAdminClientService(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    public String getAdminToken() {
        String tokenUrl = baseUrl + "/realms/" + realm + "/protocol/openid-connect/token";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", "password");
        formData.add("client_id", clientId);
        formData.add("client_secret", clientSecret);
        formData.add("username", adminUsername);
        formData.add("password", adminPassword);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(formData, headers);
        ResponseEntity<String> response = restTemplate.postForEntity(tokenUrl, request, String.class);

        if (response.getStatusCode().is2xxSuccessful()) {
            try {
                JsonNode jsonNode = objectMapper.readTree(response.getBody());
                return jsonNode.get("access_token").asText();
            } catch (Exception e) {
                throw new RuntimeException("Lỗi khi đọc access token", e);
            }
        }

        throw new RuntimeException("Không thể lấy access token từ Keycloak: " + response.getStatusCode());
    }

    public void createUser(String username, String email, String password, String roleName) {
        String token = getAdminToken();

        // 1. Tạo user
        String createUserUrl = baseUrl + "/admin/realms/" + realm + "/users";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);

        Map<String, Object> userPayload = Map.of(
                "username", username,
                "email", email,
                "enabled", true,
                "credentials", List.of(
                        Map.of(
                                "type", "password",
                                "value", password,
                                "temporary", false
                        )
                )
        );

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(userPayload, headers);
        ResponseEntity<String> response = restTemplate.postForEntity(createUserUrl, request, String.class);

        if (!response.getStatusCode().is2xxSuccessful() && response.getStatusCode() != HttpStatus.CREATED) {
            throw new RuntimeException("Tạo user thất bại: " + response.getStatusCode() + " - " + response.getBody());
        }

        // 2. Lấy ID user vừa tạo
        String userId = getUserIdByUsername(username, token);
        if (userId == null) {
            throw new RuntimeException("Không tìm thấy user vừa tạo với username: " + username);
        }

        // 3. Gán role
        assignRealmRoleToUser(userId, roleName, token);
    }

    private String getUserIdByUsername(String username, String token) {
        String url = baseUrl + "/admin/realms/" + realm + "/users?username=" + username;
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        HttpEntity<Void> request = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, request, String.class);
        if (response.getStatusCode().is2xxSuccessful()) {
            try {
                JsonNode array = objectMapper.readTree(response.getBody());
                if (array.isArray() && !array.isEmpty()) {
                    return array.get(0).get("id").asText();
                }
            } catch (Exception e) {
                throw new RuntimeException("Lỗi khi đọc userId từ phản hồi", e);
            }
        }

        return null;
    }

    private void assignRealmRoleToUser(String userId, String roleName, String token) {
        String url = baseUrl + "/admin/realms/" + realm + "/users/" + userId + "/role-mappings/realm";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);

        // Lấy role representation
        String roleUrl = baseUrl + "/admin/realms/" + realm + "/roles/" + roleName;
        HttpEntity<Void> roleRequest = new HttpEntity<>(headers);
        ResponseEntity<String> roleResponse = restTemplate.exchange(roleUrl, HttpMethod.GET, roleRequest, String.class);

        if (!roleResponse.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("Không tìm thấy role: " + roleName);
        }

        JsonNode roleNode;
        try {
            roleNode = objectMapper.readTree(roleResponse.getBody());
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi parse role", e);
        }

        List<Map<String, Object>> roleList = List.of(
                Map.of(
                        "id", roleNode.get("id").asText(),
                        "name", roleNode.get("name").asText()
                )
        );

        HttpEntity<List<Map<String, Object>>> assignRequest = new HttpEntity<>(roleList, headers);
        ResponseEntity<Void> assignResponse = restTemplate.exchange(url, HttpMethod.POST, assignRequest, Void.class);

        if (!assignResponse.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("Gán role thất bại: " + assignResponse.getStatusCode());
        }
    }
}
