package com.medisphere.auth.service;

import com.medisphere.audit.model.AuditAction;
import com.medisphere.audit.model.AuditOutcome;
import com.medisphere.audit.service.AuditService;
import com.medisphere.auth.dto.LoginRequest;
import com.medisphere.auth.dto.LoginResponse;
import com.medisphere.auth.dto.RegisterRequest;
import com.medisphere.auth.dto.UserDTO;
import com.medisphere.auth.model.User;
import com.medisphere.auth.repository.UserRepository;
import com.medisphere.auth.security.JwtTokenProvider;
import com.medisphere.common.exception.ResourceNotFoundException;
import com.medisphere.common.exception.UserAlreadyExistsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service managing user authentication, registration, and profile retrieval.
 */
@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final AuditService auditService;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtTokenProvider tokenProvider,
                       AuditService auditService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
        this.auditService = auditService;
    }

    /**
     * Authenticates a user with username and password, returning a JWT token with SMART scopes.
     */
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElse(null);

        if (user == null) {
            log.warn("Login attempt failed: user '{}' not found", request.getUsername());
            auditService.logAnonymous(request.getUsername(), AuditAction.USER_LOGIN_FAILED,
                    "AUTH", null, null, "Failed login attempt: username not found", AuditOutcome.FAILURE);
            throw new BadCredentialsException("Invalid username or password");
        }

        if (!user.isActive()) {
            log.warn("Login attempt failed: user '{}' is inactive", request.getUsername());
            auditService.logAnonymous(request.getUsername(), AuditAction.USER_LOGIN_FAILED,
                    "AUTH", user.getId(), user.getLinkedPatientId(), "Failed login attempt: user is inactive", AuditOutcome.FAILURE);
            throw new BadCredentialsException("User account is inactive");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            log.warn("Login attempt failed: invalid password for user '{}'", request.getUsername());
            auditService.logAnonymous(request.getUsername(), AuditAction.USER_LOGIN_FAILED,
                    "AUTH", user.getId(), user.getLinkedPatientId(), "Failed login attempt: invalid password", AuditOutcome.FAILURE);
            throw new BadCredentialsException("Invalid username or password");
        }

        String token = tokenProvider.generateToken(user);
        UserDTO userDTO = toDTO(user);
        long expiresInSeconds = tokenProvider.getExpirationTimeMs() / 1000;

        auditService.log(
                user.getId(),
                user.getUsername(),
                user.getRole().name(),
                AuditAction.USER_LOGIN,
                "AUTH",
                user.getId(),
                user.getLinkedPatientId(),
                "User successfully logged in with role " + user.getRole(),
                AuditOutcome.SUCCESS,
                null
        );

        log.info("User '{}' successfully authenticated with role [{}]", user.getUsername(), user.getRole());
        return new LoginResponse(token, "Bearer", expiresInSeconds, userDTO);
    }

    /**
     * Registers a new user account.
     * Allowed only for ADMIN role. Hashes the password securely with BCrypt.
     */
    public UserDTO register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new UserAlreadyExistsException("Username '" + request.getUsername() + "' is already taken");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new UserAlreadyExistsException("Email '" + request.getEmail() + "' is already registered");
        }

        String encodedPassword = passwordEncoder.encode(request.getPassword());
        User newUser = new User(
                request.getUsername(),
                request.getEmail(),
                encodedPassword,
                request.getRole(),
                request.getLinkedPatientId(),
                request.getLinkedProviderId()
        );

        User savedUser = userRepository.save(newUser);
        log.info("Created new user '{}' with role [{}]", savedUser.getUsername(), savedUser.getRole());
        return toDTO(savedUser);
    }

    /**
     * Retrieves the profile of the currently authenticated user.
     */
    public UserDTO getCurrentUser(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
        return toDTO(user);
    }

    /**
     * Converts a User entity to a safe UserDTO with role-specific SMART scopes.
     */
    public UserDTO toDTO(User user) {
        List<String> scopes = tokenProvider.getScopesForRole(user.getRole());
        return new UserDTO(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole(),
                user.getLinkedPatientId(),
                user.getLinkedProviderId(),
                scopes
        );
    }
}
