package com.medisphere.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.medisphere.auth.dto.LoginRequest;
import com.medisphere.auth.dto.LoginResponse;
import com.medisphere.auth.dto.RegisterRequest;
import com.medisphere.auth.dto.UserDTO;
import com.medisphere.auth.model.Role;
import com.medisphere.auth.security.JwtAuthFilter;
import com.medisphere.auth.security.JwtTokenProvider;
import com.medisphere.auth.service.AuthService;
import com.medisphere.common.controller.StatusController;
import com.medisphere.config.SecurityConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {AuthController.class, StatusController.class})
@Import({SecurityConfig.class, JwtAuthFilter.class})
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @Test
    @DisplayName("GET /api/status is public and accessible without authentication")
    void testPublicStatusEndpoint() throws Exception {
        mockMvc.perform(get("/api/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    @DisplayName("POST /api/auth/login with valid credentials returns 200 and Bearer token")
    void testLoginSuccess() throws Exception {
        LoginRequest request = new LoginRequest("dr_smith", "Provider@123");
        UserDTO userDTO = new UserDTO("uid-1", "dr_smith", "dr.smith@hospital.org",
                Role.PROVIDER, null, "prov-001", List.of("patient/*.read", "patient/*.write"));
        LoginResponse response = new LoginResponse("sample.jwt.token", "Bearer", 86400, userDTO);

        when(authService.login(any(LoginRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("sample.jwt.token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.user.username").value("dr_smith"))
                .andExpect(jsonPath("$.user.role").value("PROVIDER"));
    }

    @Test
    @DisplayName("POST /api/auth/login with invalid credentials returns 401 Unauthorized")
    void testLoginFailure() throws Exception {
        LoginRequest request = new LoginRequest("dr_smith", "wrong_password");

        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new BadCredentialsException("Invalid username or password"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    @DisplayName("GET /api/auth/me without token returns 401 Unauthorized")
    void testGetMeUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/auth/me with valid token returns user details")
    @WithMockUser(username = "dr_smith", roles = {"PROVIDER"})
    void testGetMeAuthenticated() throws Exception {
        UserDTO userDTO = new UserDTO("uid-1", "dr_smith", "dr.smith@hospital.org",
                Role.PROVIDER, null, "prov-001", List.of("patient/*.read", "patient/*.write"));

        when(authService.getCurrentUser("dr_smith")).thenReturn(userDTO);

        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("dr_smith"))
                .andExpect(jsonPath("$.role").value("PROVIDER"))
                .andExpect(jsonPath("$.linkedProviderId").value("prov-001"));
    }

    @Test
    @DisplayName("POST /api/auth/register without authentication returns 401 Unauthorized")
    void testRegisterUnauthenticated() throws Exception {
        RegisterRequest request = new RegisterRequest("new_user", "new@test.org", "password123",
                Role.PATIENT, "pat-999", null);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/auth/register as PROVIDER returns 403 Forbidden (RBAC)")
    @WithMockUser(username = "dr_smith", roles = {"PROVIDER"})
    void testRegisterForbiddenForProvider() throws Exception {
        RegisterRequest request = new RegisterRequest("new_user", "new@test.org", "password123",
                Role.PATIENT, "pat-999", null);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/auth/register as PATIENT returns 403 Forbidden (RBAC)")
    @WithMockUser(username = "john_doe", roles = {"PATIENT"})
    void testRegisterForbiddenForPatient() throws Exception {
        RegisterRequest request = new RegisterRequest("new_user", "new@test.org", "password123",
                Role.PATIENT, "pat-999", null);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/auth/register as ADMIN succeeds and returns 201 Created")
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void testRegisterSuccessForAdmin() throws Exception {
        RegisterRequest request = new RegisterRequest("new_patient", "patient@test.org", "password123",
                Role.PATIENT, "pat-999", null);

        UserDTO created = new UserDTO("uid-2", "new_patient", "patient@test.org",
                Role.PATIENT, "pat-999", null, List.of("patient/*.read"));

        when(authService.register(any(RegisterRequest.class))).thenReturn(created);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("new_patient"))
                .andExpect(jsonPath("$.role").value("PATIENT"));
    }
}
