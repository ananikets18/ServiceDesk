package com.servicedesk.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.servicedesk.dto.CreateUserRequest;
import com.servicedesk.dto.LoginRequest;
import com.servicedesk.dto.RegisterRequest;
import com.servicedesk.entity.Role;
import com.servicedesk.repository.CommentRepository;
import com.servicedesk.repository.TicketRepository;
import com.servicedesk.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * API tests for the admin-only user management endpoints.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private CommentRepository commentRepository;

    private String requesterToken;
    private String adminToken;

    @BeforeEach
    void setUp() throws Exception {
        commentRepository.deleteAll();
        ticketRepository.deleteAll();
        userRepository.deleteAll();

        requesterToken = register("Riya Sharma", "riya@servicedesk.local", Role.REQUESTER);
        register("Arjun Mehta", "arjun@servicedesk.local", Role.AGENT);
        adminToken = register("Admin", "admin@servicedesk.local", Role.ADMIN);
    }

    private String register(String name, String email, Role role) throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setName(name);
        request.setEmail(email);
        request.setPassword("password123");
        request.setRole(role);

        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString()).get("token").asText();
    }

    private Long userId(String email) {
        return userRepository.findByEmail(email).orElseThrow().getId();
    }

    private LoginRequest login(String email) {
        LoginRequest request = new LoginRequest();
        request.setEmail(email);
        request.setPassword("password123");
        return request;
    }

    @Test
    @DisplayName("an admin can list every user")
    void adminCanListUsers() throws Exception {
        mockMvc.perform(get("/api/v1/users")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3));
    }

    @Test
    @DisplayName("a requester cannot list users")
    void requesterCannotListUsers() throws Exception {
        mockMvc.perform(get("/api/v1/users")
                        .header("Authorization", "Bearer " + requesterToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("an admin can create a user and that user can log in")
    void adminCanCreateUser() throws Exception {
        CreateUserRequest request = new CreateUserRequest();
        request.setName("Neha Kulkarni");
        request.setEmail("neha@servicedesk.local");
        request.setPassword("secret123");
        request.setRole(Role.AGENT);

        mockMvc.perform(post("/api/v1/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("neha@servicedesk.local"))
                .andExpect(jsonPath("$.role").value("AGENT"))
                .andExpect(jsonPath("$.active").value(true));

        LoginRequest newLogin = new LoginRequest();
        newLogin.setEmail("neha@servicedesk.local");
        newLogin.setPassword("secret123");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newLogin)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("creating a user with an email that is taken returns 409")
    void adminCannotCreateDuplicateUser() throws Exception {
        CreateUserRequest request = new CreateUserRequest();
        request.setName("Someone Else");
        request.setEmail("riya@servicedesk.local");
        request.setPassword("secret123");
        request.setRole(Role.REQUESTER);

        mockMvc.perform(post("/api/v1/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("deactivating a user stops them from logging in")
    void deactivatedUserCannotLogin() throws Exception {
        Long agentId = userId("arjun@servicedesk.local");

        mockMvc.perform(patch("/api/v1/users/" + agentId + "/deactivate")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login("arjun@servicedesk.local"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("a deactivated user's existing token stops working")
    void deactivatedUserTokenIsRejected() throws Exception {
        String agentToken = register("Temp Agent", "temp@servicedesk.local", Role.AGENT);
        Long agentId = userId("temp@servicedesk.local");

        mockMvc.perform(patch("/api/v1/users/" + agentId + "/deactivate")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/tickets")
                        .header("Authorization", "Bearer " + agentToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("an admin can reactivate a user")
    void adminCanReactivateUser() throws Exception {
        Long agentId = userId("arjun@servicedesk.local");

        mockMvc.perform(patch("/api/v1/users/" + agentId + "/deactivate")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/v1/users/" + agentId + "/activate")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    @DisplayName("an admin cannot deactivate their own account")
    void adminCannotDeactivateSelf() throws Exception {
        Long adminId = userId("admin@servicedesk.local");

        mockMvc.perform(patch("/api/v1/users/" + adminId + "/deactivate")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isForbidden());
    }
}
