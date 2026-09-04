package com.medisphere.auth.security;

import com.medisphere.auth.model.Role;
import com.medisphere.auth.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenProviderTest {

    private JwtTokenProvider tokenProvider;
    private final String testSecret = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";
    private final long testExpirationMs = 3600000L; // 1 hour

    @BeforeEach
    void setUp() {
        tokenProvider = new JwtTokenProvider();
        ReflectionTestUtils.setField(tokenProvider, "jwtSecret", testSecret);
        ReflectionTestUtils.setField(tokenProvider, "jwtExpirationMs", testExpirationMs);
        tokenProvider.init();
    }

    @Test
    @DisplayName("Generate token contains valid claims and user identity")
    void testGenerateTokenAndValidate() {
        User user = new User("dr_smith", "dr.smith@hospital.org", "hashed", Role.PROVIDER, null, "prov-001");
        user.setId("user-123");

        String token = tokenProvider.generateToken(user);
        assertNotNull(token);
        assertTrue(tokenProvider.validateToken(token));

        Claims claims = tokenProvider.getClaims(token);
        assertEquals("dr_smith", claims.getSubject());
        assertEquals("user-123", claims.get("userId"));
        assertEquals("PROVIDER", claims.get("role"));
        assertEquals("prov-001", claims.get("linkedProviderId"));
        assertNull(claims.get("linkedPatientId"));

        assertEquals("dr_smith", tokenProvider.getUsername(token));
    }

    @Test
    @DisplayName("ADMIN role receives only ADMIN-appropriate SMART-style scopes")
    void testAdminScopes() {
        List<String> scopes = tokenProvider.getScopesForRole(Role.ADMIN);
        assertTrue(scopes.contains("system/*.read"));
        assertTrue(scopes.contains("system/*.write"));
        assertTrue(scopes.contains("user/*.read"));
        assertTrue(scopes.contains("user/*.write"));
        assertFalse(scopes.contains("patient/*.write"), "Admin should not have clinical write scopes");
    }

    @Test
    @DisplayName("PROVIDER role receives only PROVIDER-appropriate SMART-style scopes")
    void testProviderScopes() {
        List<String> scopes = tokenProvider.getScopesForRole(Role.PROVIDER);
        assertTrue(scopes.contains("user/*.read"));
        assertTrue(scopes.contains("patient/*.read"));
        assertTrue(scopes.contains("patient/*.write"));
        assertFalse(scopes.contains("system/*.write"), "Provider should not have system admin write scope");
    }

    @Test
    @DisplayName("PATIENT role receives only PATIENT-appropriate SMART-style scopes")
    void testPatientScopes() {
        List<String> scopes = tokenProvider.getScopesForRole(Role.PATIENT);
        assertEquals(1, scopes.size());
        assertTrue(scopes.contains("patient/*.read"));
        assertFalse(scopes.contains("patient/*.write"));
        assertFalse(scopes.contains("user/*.write"));
        assertFalse(scopes.contains("system/*.write"));
    }

    @Test
    @DisplayName("Reject tampered or malformed token")
    void testTamperedToken() {
        User user = new User("admin", "admin@medisphere.local", "hashed", Role.ADMIN, null, null);
        String token = tokenProvider.generateToken(user);

        // Tamper with signature
        String tampered = token.substring(0, token.length() - 5) + "abcde";
        assertFalse(tokenProvider.validateToken(tampered));

        // Malformed string
        assertFalse(tokenProvider.validateToken("not.a.valid.jwt"));
        assertFalse(tokenProvider.validateToken(""));
        assertFalse(tokenProvider.validateToken(null));
    }

    @Test
    @DisplayName("Reject expired token")
    void testExpiredToken() {
        // Create an already expired token manually with the same key
        Date past = new Date(System.currentTimeMillis() - 10000);
        Date pastExpiry = new Date(System.currentTimeMillis() - 5000);

        String expiredToken = Jwts.builder()
                .subject("expired_user")
                .issuedAt(past)
                .expiration(pastExpiry)
                .signWith(Keys.hmacShaKeyFor(testSecret.getBytes(StandardCharsets.UTF_8)))
                .compact();

        assertFalse(tokenProvider.validateToken(expiredToken));
    }
}
