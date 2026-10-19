package com.servicedesk.service;

import com.servicedesk.dto.AssignTicketRequest;
import com.servicedesk.dto.CommentRequest;
import com.servicedesk.dto.StatusUpdateRequest;
import com.servicedesk.dto.TicketRequest;
import com.servicedesk.dto.TicketResponse;
import com.servicedesk.entity.Comment;
import com.servicedesk.entity.Priority;
import com.servicedesk.entity.Role;
import com.servicedesk.entity.Status;
import com.servicedesk.entity.Ticket;
import com.servicedesk.entity.User;
import com.servicedesk.exception.ForbiddenException;
import com.servicedesk.exception.ResourceNotFoundException;
import com.servicedesk.repository.CommentRepository;
import com.servicedesk.repository.TicketRepository;
import com.servicedesk.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the ticket business rules. The repositories are mocked, so
 * these tests are fast and only exercise TicketService itself.
 */
@ExtendWith(MockitoExtension.class)
class TicketServiceTest {

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private TicketService ticketService;

    private User requester;
    private User agent;
    private User otherAgent;
    private User admin;
    private Ticket ticket;

    @BeforeEach
    void setUp() {
        requester = user(1L, "Riya Sharma", "riya@servicedesk.local", Role.REQUESTER);
        agent = user(2L, "Arjun Mehta", "arjun@servicedesk.local", Role.AGENT);
        otherAgent = user(3L, "Neha Kulkarni", "neha@servicedesk.local", Role.AGENT);
        admin = user(4L, "Admin", "admin@servicedesk.local", Role.ADMIN);

        ticket = new Ticket("Laptop will not boot", "Blue screen on startup", Priority.HIGH, requester);
        ticket.setId(10L);
        ticket.setStatus(Status.OPEN);
    }

    private User user(Long id, String name, String email, Role role) {
        User user = new User(name, email, "hash", role);
        user.setId(id);
        return user;
    }

    private UserPrincipal principal(User user) {
        return new UserPrincipal(user);
    }

    private TicketRequest ticketRequest() {
        TicketRequest request = new TicketRequest();
        request.setTitle("VPN not connecting");
        request.setDescription("Remote access fails after password reset");
        request.setPriority(Priority.MEDIUM);
        return request;
    }

    // ---------------------------------------------------------------- create

    @Test
    @DisplayName("createTicket uses the logged-in user as the requester")
    void createTicketUsesLoggedInUser() {
        when(userService.getById(1L)).thenReturn(requester);
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> {
            Ticket saved = invocation.getArgument(0);
            saved.setId(11L);
            return saved;
        });

        TicketResponse response = ticketService.createTicket(ticketRequest(), principal(requester));

        assertThat(response.getId()).isEqualTo(11L);
        assertThat(response.getTitle()).isEqualTo("VPN not connecting");
        assertThat(response.getRequesterId()).isEqualTo(1L);
        assertThat(response.getRequesterName()).isEqualTo("Riya Sharma");
        assertThat(response.getAssigneeId()).isNull();
        verify(ticketRepository).save(any(Ticket.class));
    }

    @Test
    @DisplayName("createTicket blocks an agent from raising tickets")
    void createTicketBlocksAgent() {
        assertThatThrownBy(() -> ticketService.createTicket(ticketRequest(), principal(agent)))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Only requesters");

        verify(ticketRepository, never()).save(any(Ticket.class));
    }

    @Test
    @DisplayName("createTicket blocks an admin from raising tickets")
    void createTicketBlocksAdmin() {
        assertThatThrownBy(() -> ticketService.createTicket(ticketRequest(), principal(admin)))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Only requesters");

        verify(ticketRepository, never()).save(any(Ticket.class));
    }

    // ------------------------------------------------------------------ list

    @Test
    @DisplayName("getAllTickets returns only the requester's own tickets")
    void getAllTicketsForRequesterOnlyReturnsOwnTickets() {
        when(ticketRepository.findByRequesterId(1L)).thenReturn(List.of(ticket));

        List<TicketResponse> result = ticketService.getAllTickets(principal(requester));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(10L);
        verify(ticketRepository, never()).findAll();
    }

    @Test
    @DisplayName("getAllTickets returns every ticket for an agent")
    void getAllTicketsForAgentReturnsEverything() {
        when(ticketRepository.findAll()).thenReturn(List.of(ticket));

        List<TicketResponse> result = ticketService.getAllTickets(principal(agent));

        assertThat(result).hasSize(1);
        verify(ticketRepository).findAll();
        verify(ticketRepository, never()).findByRequesterId(any());
    }

    // ------------------------------------------------------------------- get

    @Test
    @DisplayName("getTicketById throws 404 when the ticket does not exist")
    void getTicketByIdThrowsWhenMissing() {
        when(ticketRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ticketService.getTicketById(99L, principal(requester)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    @DisplayName("getTicketById blocks a requester from reading someone else's ticket")
    void getTicketByIdBlocksOtherRequestersTicket() {
        User someoneElse = user(5L, "Other Requester", "other@servicedesk.local", Role.REQUESTER);
        when(ticketRepository.findById(10L)).thenReturn(Optional.of(ticket));

        assertThatThrownBy(() -> ticketService.getTicketById(10L, principal(someoneElse)))
                .isInstanceOf(ForbiddenException.class);
    }

    // ---------------------------------------------------------------- status

    @Test
    @DisplayName("updateStatus lets an assigned agent change the status")
    void updateStatusWorksForAssignedAgent() {
        ticket.setAssignee(agent);
        when(ticketRepository.findById(10L)).thenReturn(Optional.of(ticket));
        when(ticketRepository.save(ticket)).thenReturn(ticket);

        StatusUpdateRequest request = new StatusUpdateRequest();
        request.setStatus(Status.IN_PROGRESS);

        TicketResponse response = ticketService.updateStatus(10L, request, principal(agent));

        assertThat(response.getStatus()).isEqualTo(Status.IN_PROGRESS);
        verify(ticketRepository).save(ticket);
    }

    @Test
    @DisplayName("updateStatus blocks a requester")
    void updateStatusBlocksRequester() {
        when(ticketRepository.findById(10L)).thenReturn(Optional.of(ticket));

        StatusUpdateRequest request = new StatusUpdateRequest();
        request.setStatus(Status.CLOSED);

        assertThatThrownBy(() -> ticketService.updateStatus(10L, request, principal(requester)))
                .isInstanceOf(ForbiddenException.class);
        verify(ticketRepository, never()).save(any(Ticket.class));
    }

    @Test
    @DisplayName("updateStatus blocks an agent who is not assigned to the ticket")
    void updateStatusBlocksUnassignedAgent() {
        ticket.setAssignee(otherAgent);
        when(ticketRepository.findById(10L)).thenReturn(Optional.of(ticket));

        StatusUpdateRequest request = new StatusUpdateRequest();
        request.setStatus(Status.RESOLVED);

        assertThatThrownBy(() -> ticketService.updateStatus(10L, request, principal(agent)))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("assigned to them");
    }

    @Test
    @DisplayName("updateStatus allows an admin on any ticket")
    void updateStatusAllowsAdmin() {
        ticket.setAssignee(otherAgent);
        when(ticketRepository.findById(10L)).thenReturn(Optional.of(ticket));
        when(ticketRepository.save(ticket)).thenReturn(ticket);

        StatusUpdateRequest request = new StatusUpdateRequest();
        request.setStatus(Status.RESOLVED);

        TicketResponse response = ticketService.updateStatus(10L, request, principal(admin));

        assertThat(response.getStatus()).isEqualTo(Status.RESOLVED);
    }

    // ---------------------------------------------------------------- assign

    @Test
    @DisplayName("assignTicket lets an agent assign a ticket to themselves")
    void assignTicketAllowsSelfAssignment() {
        when(ticketRepository.findById(10L)).thenReturn(Optional.of(ticket));
        when(userService.getById(2L)).thenReturn(agent);
        when(ticketRepository.save(ticket)).thenReturn(ticket);

        AssignTicketRequest request = new AssignTicketRequest();
        request.setAssigneeId(2L);

        TicketResponse response = ticketService.assignTicket(10L, request, principal(agent));

        assertThat(response.getAssigneeId()).isEqualTo(2L);
        assertThat(response.getAssigneeName()).isEqualTo("Arjun Mehta");
    }

    @Test
    @DisplayName("assignTicket blocks an agent from assigning someone else")
    void assignTicketBlocksAssigningSomeoneElse() {
        when(ticketRepository.findById(10L)).thenReturn(Optional.of(ticket));

        AssignTicketRequest request = new AssignTicketRequest();
        request.setAssigneeId(3L);

        assertThatThrownBy(() -> ticketService.assignTicket(10L, request, principal(agent)))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("themselves");
    }

    @Test
    @DisplayName("assignTicket blocks a requester")
    void assignTicketBlocksRequester() {
        when(ticketRepository.findById(10L)).thenReturn(Optional.of(ticket));

        AssignTicketRequest request = new AssignTicketRequest();
        request.setAssigneeId(2L);

        assertThatThrownBy(() -> ticketService.assignTicket(10L, request, principal(requester)))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    @DisplayName("assignTicket rejects assigning to a requester")
    void assignTicketRejectsRequesterAsAssignee() {
        when(ticketRepository.findById(10L)).thenReturn(Optional.of(ticket));
        when(userService.getById(1L)).thenReturn(requester);

        AssignTicketRequest request = new AssignTicketRequest();
        request.setAssigneeId(1L);

        assertThatThrownBy(() -> ticketService.assignTicket(10L, request, principal(admin)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("agent or an admin");
    }

    // --------------------------------------------------------------- comment

    @Test
    @DisplayName("addComment uses the logged-in user as the author")
    void addCommentUsesLoggedInUser() {
        when(ticketRepository.findById(10L)).thenReturn(Optional.of(ticket));
        when(userService.getById(2L)).thenReturn(agent);
        when(commentRepository.save(any(Comment.class))).thenAnswer(invocation -> {
            Comment saved = invocation.getArgument(0);
            saved.setId(7L);
            return saved;
        });

        CommentRequest request = new CommentRequest();
        request.setMessage("Looking into this now");

        var response = ticketService.addComment(10L, request, principal(agent));

        assertThat(response.getId()).isEqualTo(7L);
        assertThat(response.getAuthorId()).isEqualTo(2L);
        assertThat(response.getAuthorName()).isEqualTo("Arjun Mehta");
        assertThat(response.getTicketId()).isEqualTo(10L);
        assertThat(response.getMessage()).isEqualTo("Looking into this now");
    }

    @Test
    @DisplayName("addComment blocks a requester from commenting on someone else's ticket")
    void addCommentBlocksOtherRequestersTicket() {
        User someoneElse = user(5L, "Other Requester", "other@servicedesk.local", Role.REQUESTER);
        when(ticketRepository.findById(10L)).thenReturn(Optional.of(ticket));

        CommentRequest request = new CommentRequest();
        request.setMessage("Trying to sneak in");

        assertThatThrownBy(() -> ticketService.addComment(10L, request, principal(someoneElse)))
                .isInstanceOf(ForbiddenException.class);
        verify(commentRepository, never()).save(any(Comment.class));
    }
}
