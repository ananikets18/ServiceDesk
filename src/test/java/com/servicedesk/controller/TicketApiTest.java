package com.servicedesk.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.servicedesk.dto.LoginRequest;
import com.servicedesk.dto.RegisterRequest;
import com.servicedesk.dto.TicketRequest;
import com.servicedesk.entity.Priority;
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
 * API tests that go through the real HTTP layer, security filters and database.
 * The test profile swaps PostgreSQL for an in-memory H2 database.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TicketApiTest {

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
    private String agentToken;
    private String adminToken;

    @BeforeEach
    void setUp() throws Exception {
        commentRepository.deleteAll();
        ticketRepository.deleteAll();
        userRepository.deleteAll();

        requesterToken = register("Riya Sharma", "riya@servicedesk.local", Role.REQUESTER);
        agentToken = register("Arjun Mehta", "arjun@servicedesk.local", Role.AGENT);
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

    private Long createTicket(String token, String title) throws Exception {
        TicketRequest request = new TicketRequest();
        request.setTitle(title);
        request.setDescription("Something is broken and needs attention");
        request.setPriority(Priority.HIGH);

        MvcResult result = mockMvc.perform(post("/api/v1/tickets")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private Long userId(String email) {
        return userRepository.findByEmail(email).orElseThrow().getId();
    }

    // -------------------------------------------------------------- auth

    @Test
    @DisplayName("register returns 201 and a token")
    void registerReturnsToken() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setName("New Person");
        request.setEmail("new.person@servicedesk.local");
        request.setPassword("password123");
        request.setRole(Role.REQUESTER);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.email").value("new.person@servicedesk.local"))
                .andExpect(jsonPath("$.role").value("REQUESTER"));
    }

    @Test
    @DisplayName("login returns 200 and a token for valid credentials")
    void loginReturnsToken() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail("riya@servicedesk.local");
        request.setPassword("password123");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    // ------------------------------------------------------------ tickets

    @Test
    @DisplayName("health check is public")
    void healthIsPublic() throws Exception {
        mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("creating a ticket requires authentication")
    void createTicketRequiresAuthentication() throws Exception {
        TicketRequest request = new TicketRequest();
        request.setTitle("No token");
        request.setDescription("Should be rejected");
        request.setPriority(Priority.LOW);

        mockMvc.perform(post("/api/v1/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("a requester can create a ticket and it starts as OPEN")
    void requesterCanCreateTicket() throws Exception {
        TicketRequest request = new TicketRequest();
        request.setTitle("Laptop will not boot");
        request.setDescription("Blue screen on startup since this morning");
        request.setPriority(Priority.HIGH);

        mockMvc.perform(post("/api/v1/tickets")
                        .header("Authorization", "Bearer " + requesterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Laptop will not boot"))
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.requesterName").value("Riya Sharma"))
                .andExpect(jsonPath("$.assigneeId").doesNotExist());
    }

    @Test
    @DisplayName("a requester only sees their own tickets")
    void requesterOnlySeesOwnTickets() throws Exception {
        createTicket(requesterToken, "Riya's ticket");

        mockMvc.perform(get("/api/v1/tickets")
                        .header("Authorization", "Bearer " + requesterToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].title").value("Riya's ticket"));
    }

    @Test
    @DisplayName("an agent sees tickets from every requester")
    void agentSeesAllTickets() throws Exception {
        createTicket(requesterToken, "Riya's ticket");
        String otherRequester = register("Other Requester", "other@servicedesk.local", Role.REQUESTER);
        createTicket(otherRequester, "Other ticket");

        mockMvc.perform(get("/api/v1/tickets")
                        .header("Authorization", "Bearer " + agentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    @DisplayName("an agent cannot raise a ticket")
    void agentCannotRaiseTicket() throws Exception {
        TicketRequest request = new TicketRequest();
        request.setTitle("Agent raising a ticket");
        request.setDescription("Agents should not do this");
        request.setPriority(Priority.LOW);

        mockMvc.perform(post("/api/v1/tickets")
                        .header("Authorization", "Bearer " + agentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("an admin cannot raise a ticket")
    void adminCannotRaiseTicket() throws Exception {
        TicketRequest request = new TicketRequest();
        request.setTitle("Admin raising a ticket");
        request.setDescription("Admins should not do this either");
        request.setPriority(Priority.LOW);

        mockMvc.perform(post("/api/v1/tickets")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("an agent can list the agents")
    void agentCanListAgents() throws Exception {
        mockMvc.perform(get("/api/v1/users/agents")
                        .header("Authorization", "Bearer " + agentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].role").value("AGENT"));
    }

    @Test
    @DisplayName("a requester cannot list the agents")
    void requesterCannotListAgents() throws Exception {
        mockMvc.perform(get("/api/v1/users/agents")
                        .header("Authorization", "Bearer " + requesterToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("an agent can assign a ticket to themselves and update its status")
    void agentCanAssignAndUpdateStatus() throws Exception {
        Long ticketId = createTicket(requesterToken, "VPN not connecting");

        mockMvc.perform(patch("/api/v1/tickets/" + ticketId + "/assign")
                        .header("Authorization", "Bearer " + agentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"assigneeId\":" + userId("arjun@servicedesk.local") + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assigneeName").value("Arjun Mehta"));

        mockMvc.perform(patch("/api/v1/tickets/" + ticketId + "/status")
                        .header("Authorization", "Bearer " + agentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"IN_PROGRESS\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }

    @Test
    @DisplayName("a status jump that skips a step returns 400")
    void statusJumpIsRejected() throws Exception {
        Long ticketId = createTicket(requesterToken, "Needs careful handling");

        mockMvc.perform(patch("/api/v1/tickets/" + ticketId + "/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CLOSED\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Cannot move a ticket")));
    }

    @Test
    @DisplayName("a requester can edit the title and description of their own ticket")
    void requesterCanEditOwnTicket() throws Exception {
        Long ticketId = createTicket(requesterToken, "Printer offline");

        mockMvc.perform(patch("/api/v1/tickets/" + ticketId)
                        .header("Authorization", "Bearer " + requesterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Printer still offline\",\"description\":\"Tried a new cable\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Printer still offline"))
                .andExpect(jsonPath("$.description").value("Tried a new cable"));
    }

    @Test
    @DisplayName("an agent cannot edit a ticket")
    void agentCannotEditTicket() throws Exception {
        Long ticketId = createTicket(requesterToken, "Leave this alone");

        mockMvc.perform(patch("/api/v1/tickets/" + ticketId)
                        .header("Authorization", "Bearer " + agentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Agent edit\",\"description\":\"Should be blocked\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("a requester can add a comment to their own ticket")
    void requesterCanComment() throws Exception {
        Long ticketId = createTicket(requesterToken, "Printer offline");

        mockMvc.perform(post("/api/v1/tickets/" + ticketId + "/comments")
                        .header("Authorization", "Bearer " + requesterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"It is still not working\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("It is still not working"))
                .andExpect(jsonPath("$.authorName").value("Riya Sharma"));

        mockMvc.perform(get("/api/v1/tickets/" + ticketId + "/comments")
                        .header("Authorization", "Bearer " + agentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    // ------------------------------------------------- negative cases

    @Test
    @DisplayName("a blank title returns 400 with field errors")
    void blankTitleReturnsValidationError() throws Exception {
        TicketRequest request = new TicketRequest();
        request.setTitle("   ");
        request.setDescription("Description is fine");
        request.setPriority(Priority.LOW);

        mockMvc.perform(post("/api/v1/tickets")
                        .header("Authorization", "Bearer " + requesterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Failed"))
                .andExpect(jsonPath("$.fieldErrors.title").value("title must not be blank"));
    }

    @Test
    @DisplayName("a missing priority returns 400")
    void missingPriorityReturnsValidationError() throws Exception {
        mockMvc.perform(post("/api/v1/tickets")
                        .header("Authorization", "Bearer " + requesterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"No priority\",\"description\":\"Oops\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.priority").exists());
    }

    @Test
    @DisplayName("fetching an unknown ticket returns 404")
    void unknownTicketReturns404() throws Exception {
        mockMvc.perform(get("/api/v1/tickets/9999")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"));
    }

    @Test
    @DisplayName("a requester cannot read someone else's ticket")
    void requesterCannotReadSomeoneElsesTicket() throws Exception {
        Long ticketId = createTicket(requesterToken, "Private ticket");
        String otherRequester = register("Other Requester", "other@servicedesk.local", Role.REQUESTER);

        mockMvc.perform(get("/api/v1/tickets/" + ticketId)
                        .header("Authorization", "Bearer " + otherRequester))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @DisplayName("a requester cannot change a ticket's status")
    void requesterCannotChangeStatus() throws Exception {
        Long ticketId = createTicket(requesterToken, "Cannot close this myself");

        mockMvc.perform(patch("/api/v1/tickets/" + ticketId + "/status")
                        .header("Authorization", "Bearer " + requesterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CLOSED\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("an agent cannot assign a ticket to another agent")
    void agentCannotAssignAnotherAgent() throws Exception {
        Long ticketId = createTicket(requesterToken, "Agent assignment rules");
        register("Neha Kulkarni", "neha@servicedesk.local", Role.AGENT);

        mockMvc.perform(patch("/api/v1/tickets/" + ticketId + "/assign")
                        .header("Authorization", "Bearer " + agentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"assigneeId\":" + userId("neha@servicedesk.local") + "}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("registering a duplicate email returns 409")
    void duplicateEmailReturns409() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setName("Copycat");
        request.setEmail("riya@servicedesk.local");
        request.setPassword("password123");
        request.setRole(Role.REQUESTER);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Conflict"));
    }

    @Test
    @DisplayName("a wrong password returns 401")
    void wrongPasswordReturns401() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail("riya@servicedesk.local");
        request.setPassword("not-the-password");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    @DisplayName("a garbage token returns 401")
    void garbageTokenReturns401() throws Exception {
        mockMvc.perform(get("/api/v1/tickets")
                        .header("Authorization", "Bearer this.is.not.a.jwt"))
                .andExpect(status().isUnauthorized());
    }
}
