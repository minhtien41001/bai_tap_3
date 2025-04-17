package com.example.book_service.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

@Component
public class PermissionChecker {

    public boolean hasAccess(HttpServletRequest request, String role) {
        String path = request.getRequestURI();
        String method = request.getMethod();

        if (path.startsWith("/api/books")) {
            if (method.equals("GET") || method.equals("POST") || method.equals("PUT")) {
                return role.equals("ADMIN") || role.equals("EMPLOYEE");
            } else if (method.equals("DELETE")) {
                return role.equals("ADMIN");
            }
        }
        return false;
    }
}
