package com.medisphere.auth.service;

import com.medisphere.auth.dto.LoginRequest;
import com.medisphere.auth.dto.LoginResponse;
import com.medisphere.auth.dto.RegisterRequest;
import com.medisphere.auth.dto.UserDTO;
import com.medisphere.auth.model.Role;
import com.medisphere.auth.model.User;
import com.medisphere.auth.repository.UserRepository;
import com.medisphere.auth.security.JwtTokenProvider;
import com.medisphere.common.exception.UserAlreadyExistsException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtTokenProvider tokenProvider;

    private PasswordEncoder passwordEncoder;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        authService = new AuthService(userRepository, passwordEncoder, tokenProvider);
    }

    @Test
    @DisplayName("Successful login returns JWT token and user info")
    void testLoginSuccess() {
        String rawPassword = "Provider@123";
        String encodedHash = passwordEncoder.encode(rawPassword);

        User user = new User("dr_smith", "dr.smith@hospital.org", encodedHash, Role.PROVIDER, null, "prov-001");
        user.setId("user-1");

        when(userRepository.findByUsername("dr_smith")).thenReturn(Optional.of(user));
        when(tokenProvider.generateToken(user)).thenReturn("mocked.jwt.token");
        when(tokenProvider.getScopesForRole(Role.PROVIDER)).thenReturn(List.of("patient/*.read", "patient/*.write"));
        when(tokenProvider.getExpirationTimeMs()).thenReturn(86400000L);

        LoginRequest request = new LoginRequest("dr_smith", rawPassword);
        LoginResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals("mocked.jwt.token", response.getToken());
        assertEquals("Bearer", response.getTokenType());
        assertEquals(86400L, response.getExpiresIn());
        assertEquals("dr_smith", response.getUser().getUsername());
        assertEquals(Role.PROVIDER, response.getUser().getRole());
    }

    @Test
    @DisplayName("Login failure with incorrect password throws BadCredentialsException")
    void testLoginWrongPassword() {
        String encodedHash = passwordEncoder.encode("CorrectPassword");
        User user = new User("dr_smith", "dr.smith@hospital.org", encodedHash, Role.PROVIDER, null, null);

        when(userRepository.findByUsername("dr_smith")).thenReturn(Optional.of(user));

        LoginRequest request = new LoginRequest("dr_smith", "WrongPassword");
        assertThrows(BadCredentialsException.class, () -> authService.login(request));
    }

    @Test
    @DisplayName("Login failure with non-existent user throws BadCredentialsException")
    void testLoginUserNotFound() {
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        LoginRequest request = new LoginRequest("unknown", "any_password");
        assertThrows(BadCredentialsException.class, () -> authService.login(request));
    }

    @Test
    @DisplayName("Login failure with inactive user throws BadCredentialsException")
    void testLoginInactiveUser() {
        String rawPassword = "Patient@123";
        User user = new User("inactive_user", "inactive@test.org", passwordEncoder.encode(rawPassword), Role.PATIENT, null, null);
        user.setActive(false);

        when(userRepository.findByUsername("inactive_user")).thenReturn(Optional.of(user));

        LoginRequest request = new LoginRequest("inactive_user", rawPassword);
        assertThrows(BadCredentialsException.class, () -> authService.login(request));
    }

    @Test
    @DisplayName("Register user hashes password with BCrypt and persists user")
    void testRegisterSuccess() {
        RegisterRequest request = new RegisterRequest(
                "new_doctor",
                "doctor@hospital.org",
                "SecurePass123",
                Role.PROVIDER,
                null,
                "prov-002"
        );

        when(userRepository.existsByUsername("new_doctor")).thenReturn(false);
        when(userRepository.existsByEmail("doctor@hospital.org")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId("generated-id");
            return u;
        });
        when(tokenProvider.getScopesForRole(Role.PROVIDER)).thenReturn(List.of("user/*.read", "patient/*.read", "patient/*.write"));

        UserDTO userDTO = authService.register(request);

        assertNotNull(userDTO);
        assertEquals("new_doctor", userDTO.getUsername());
        assertEquals("doctor@hospital.org", userDTO.getEmail());
        assertEquals(Role.PROVIDER, userDTO.getRole());
        assertEquals("prov-002", userDTO.getLinkedProviderId());

        // Verify password was securely hashed
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User captured = captor.getValue();
        assertNotEquals("SecurePass123", captured.getPasswordHash());
        assertTrue(passwordEncoder.matches("SecurePass123", captured.getPasswordHash()));
    }

    @Test
    @DisplayName("Register duplicate username throws UserAlreadyExistsException (409)")
    void testRegisterDuplicateUsername() {
        RegisterRequest request = new RegisterRequest(
                "existing_user",
                "unique@test.org",
                "password123",
                Role.PATIENT,
                null,
                null
        );

        when(userRepository.existsByUsername("existing_user")).thenReturn(true);

        assertThrows(UserAlreadyExistsException.class, () -> authService.register(request));
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Register duplicate email throws UserAlreadyExistsException (409)")
    void testRegisterDuplicateEmail() {
        RegisterRequest request = new RegisterRequest(
                "new_username",
                "existing@test.org",
                "password123",
                Role.PATIENT,
                null,
                null
        );

        when(userRepository.existsByUsername("new_username")).thenReturn(false);
        when(userRepository.existsByEmail("existing@test.org")).thenReturn(true);

        assertThrows(UserAlreadyExistsException.class, () -> authService.register(request));
        verify(userRepository, never()).save(any());
    }
}
