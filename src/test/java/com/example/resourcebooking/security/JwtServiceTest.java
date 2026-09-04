package com.example.resourcebooking.security;

import com.example.resourcebooking.entity.User;
import com.example.resourcebooking.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class JwtServiceTest {

    @Autowired
    private JwtService jwtService;

    private UserPrincipal userPrincipal;

    @BeforeEach
    void setUp() {
        User user = new User("testuser", "test@example.com", "Password@123", Role.USER, true);
        user.setId(99L);
        userPrincipal = UserPrincipal.create(user);
    }

    @Test
    @DisplayName("Should generate valid JWT token with subject and claims")
    void testGenerateToken() {
        String token = jwtService.generateToken(userPrincipal);
        assertNotNull(token);
        assertFalse(token.isBlank());

        String username = jwtService.extractUsername(token);
        assertEquals("testuser", username);

        String role = jwtService.extractRole(token);
        assertEquals("USER", role);

        Long userId = jwtService.extractUserId(token);
        assertEquals(99L, userId);

        assertTrue(jwtService.validateToken(token, userPrincipal));
        assertFalse(jwtService.isTokenExpired(token));
    }
}
