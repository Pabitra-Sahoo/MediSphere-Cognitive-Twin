package com.medisphere.auth.security;

import com.medisphere.auth.model.Role;
import com.medisphere.auth.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

/**
 * Utility component for generating, signing, and validating JSON Web Tokens (JWT).
 *
 * <p>Embeds SMART-on-FHIR-compatible scope claims aligned with the authenticated role:
 * <ul>
 *   <li>{@code ADMIN}: {@code user/*.read}, {@code user/*.write}, {@code system/*.read}, {@code system/*.write}</li>
 *   <li>{@code PROVIDER}: {@code user/*.read}, {@code patient/*.read}, {@code patient/*.write}</li>
 *   <li>{@code PATIENT}: {@code patient/*.read}</li>
 * </ul>
 * </p>
 */
@Component
public class JwtTokenProvider {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenProvider.class);

    @Value("${jwt.secret:404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970}")
    private String jwtSecret;

    @Value("${jwt.expiration-ms:86400000}")
    private long jwtExpirationMs;

    private SecretKey key;

    @PostConstruct
    public void init() {
        byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            log.warn("Configured JWT secret is shorter than 256 bits. Padding for local HMAC-SHA256 development.");
            byte[] padded = new byte[32];
            System.arraycopy(keyBytes, 0, padded, 0, Math.min(keyBytes.length, 32));
            this.key = Keys.hmacShaKeyFor(padded);
        } else {
            this.key = Keys.hmacShaKeyFor(keyBytes);
        }
    }

    /**
     * Constructs a JWT for an authenticated user.
     */
    public String generateToken(User user) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationMs);

        List<String> scopes = getScopesForRole(user.getRole());
        String scopeString = String.join(" ", scopes);

        return Jwts.builder()
                .subject(user.getUsername())
                .claim("userId", user.getId())
                .claim("username", user.getUsername())
                .claim("role", user.getRole().name())
                .claim("linkedPatientId", user.getLinkedPatientId())
                .claim("linkedProviderId", user.getLinkedProviderId())
                .claim("scope", scopeString)
                .claim("scopes", scopes)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(key)
                .compact();
    }

    /**
     * Validates a JWT's structure, cryptographic signature, and expiration time.
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (SignatureException ex) {
            log.warn("Invalid JWT signature: {}", ex.getMessage());
        } catch (MalformedJwtException ex) {
            log.warn("Malformed JWT token: {}", ex.getMessage());
        } catch (ExpiredJwtException ex) {
            log.warn("Expired JWT token: {}", ex.getMessage());
        } catch (UnsupportedJwtException ex) {
            log.warn("Unsupported JWT token: {}", ex.getMessage());
        } catch (IllegalArgumentException ex) {
            log.warn("JWT claims string is empty: {}", ex.getMessage());
        }
        return false;
    }

    /**
     * Extracts claims payload from a valid JWT.
     */
    public Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Extracts username (subject) from token.
     */
    public String getUsername(String token) {
        return getClaims(token).getSubject();
    }

    /**
     * Returns SMART-on-FHIR scopes appropriate for the role.
     */
    public List<String> getScopesForRole(Role role) {
        if (role == null) {
            return List.of();
        }
        return switch (role) {
            case ADMIN -> List.of(
                    "user/*.read",
                    "user/*.write",
                    "system/*.read",
                    "system/*.write"
            );
            case PROVIDER -> List.of(
                    "user/*.read",
                    "patient/*.read",
                    "patient/*.write"
            );
            case PATIENT -> List.of(
                    "patient/*.read"
            );
        };
    }

    public long getExpirationTimeMs() {
        return jwtExpirationMs;
    }
}
