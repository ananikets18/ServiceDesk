package com.servicedesk.service;

import com.servicedesk.dto.AssignTicketRequest;
import com.servicedesk.dto.CommentRequest;
import com.servicedesk.dto.CommentResponse;
import com.servicedesk.dto.StatusUpdateRequest;
import com.servicedesk.dto.TicketRequest;
import com.servicedesk.dto.TicketResponse;
import com.servicedesk.dto.TicketUpdateRequest;
import com.servicedesk.entity.Comment;
import com.servicedesk.entity.Role;
import com.servicedesk.entity.Status;
import com.servicedesk.entity.Ticket;
import com.servicedesk.entity.User;
import com.servicedesk.exception.ForbiddenException;
import com.servicedesk.exception.ResourceNotFoundException;
import com.servicedesk.repository.CommentRepository;
import com.servicedesk.repository.TicketRepository;
import com.servicedesk.security.UserPrincipal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * All the ticket business rules live here: creating them, reading them,
 * changing their status, assigning an agent and adding comments.
 *
 * Access rules:
 * - Requesters only see their own tickets and cannot change status or assign.
 * - Agents see every ticket, but may only move a ticket they are assigned to
 *   (or one that is still unassigned), and may only assign it to themselves.
 * - Admins can do everything.
 */
@Service
public class TicketService {

    private final TicketRepository ticketRepository;
    private final CommentRepository commentRepository;
    private final UserService userService;

    public TicketService(TicketRepository ticketRepository,
                         CommentRepository commentRepository,
                         UserService userService) {
        this.ticketRepository = ticketRepository;
        this.commentRepository = commentRepository;
        this.userService = userService;
    }

    @Transactional
    public TicketResponse createTicket(TicketRequest request, UserPrincipal currentUser) {
        if (currentUser.getRole() != Role.REQUESTER) {
            throw new ForbiddenException("Only requesters can raise tickets");
        }

        User requester = userService.getById(currentUser.getId());
        Ticket ticket = new Ticket(request.getTitle(), request.getDescription(), request.getPriority(), requester);
        Ticket saved = ticketRepository.save(ticket);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<TicketResponse> getAllTickets(UserPrincipal currentUser) {
        List<Ticket> tickets = currentUser.getRole() == Role.REQUESTER
                ? ticketRepository.findByRequesterId(currentUser.getId())
                : ticketRepository.findAll();

        return tickets.stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public TicketResponse getTicketById(Long id, UserPrincipal currentUser) {
        return toResponse(findAccessibleTicket(id, currentUser));
    }

    @Transactional
    public TicketResponse updateStatus(Long id, StatusUpdateRequest request, UserPrincipal currentUser) {
        Ticket ticket = findAccessibleTicket(id, currentUser);

        if (currentUser.getRole() == Role.REQUESTER) {
            throw new ForbiddenException("Requesters are not allowed to change ticket status");
        }
        if (currentUser.getRole() == Role.AGENT && ticket.getAssignee() != null
                && !ticket.getAssignee().getId().equals(currentUser.getId())) {
            throw new ForbiddenException("Agents can only update tickets assigned to them");
        }

        if (!ticket.getStatus().canTransitionTo(request.getStatus())) {
            throw new IllegalArgumentException(
                    "Cannot move a ticket from " + ticket.getStatus() + " to " + request.getStatus()
                            + ". Allowed next statuses: " + ticket.getStatus().allowedNextStatuses());
        }

        ticket.setStatus(request.getStatus());
        return toResponse(ticketRepository.save(ticket));
    }

    /**
     * Lets the requester who raised the ticket, or an admin, edit the title and
     * description. Agents work the ticket but do not rewrite the report.
     */
    @Transactional
    public TicketResponse updateTicket(Long id, TicketUpdateRequest request, UserPrincipal currentUser) {
        Ticket ticket = findAccessibleTicket(id, currentUser);

        boolean isOwner = ticket.getRequester().getId().equals(currentUser.getId());
        boolean isAdmin = currentUser.getRole() == Role.ADMIN;
        if (!isOwner && !isAdmin) {
            throw new ForbiddenException("Only the requester who raised this ticket, or an admin, can edit it");
        }
        if (ticket.getStatus() == Status.CLOSED) {
            throw new IllegalArgumentException("Closed tickets can no longer be edited");
        }

        ticket.setTitle(request.getTitle());
        ticket.setDescription(request.getDescription());
        return toResponse(ticketRepository.save(ticket));
    }

    @Transactional
    public TicketResponse assignTicket(Long id, AssignTicketRequest request, UserPrincipal currentUser) {
        Ticket ticket = findAccessibleTicket(id, currentUser);

        if (currentUser.getRole() == Role.REQUESTER) {
            throw new ForbiddenException("Requesters are not allowed to assign tickets");
        }
        if (currentUser.getRole() == Role.AGENT && !request.getAssigneeId().equals(currentUser.getId())) {
            throw new ForbiddenException("Agents can only assign tickets to themselves");
        }

        User assignee = userService.getById(request.getAssigneeId());
        if (assignee.getRole() == Role.REQUESTER) {
            throw new IllegalArgumentException("Tickets can only be assigned to an agent or an admin");
        }

        ticket.setAssignee(assignee);
        return toResponse(ticketRepository.save(ticket));
    }

    @Transactional
    public CommentResponse addComment(Long ticketId, CommentRequest request, UserPrincipal currentUser) {
        Ticket ticket = findAccessibleTicket(ticketId, currentUser);
        User author = userService.getById(currentUser.getId());
        Comment comment = new Comment(ticket, author, request.getMessage());
        return toCommentResponse(commentRepository.save(comment));
    }

    @Transactional(readOnly = true)
    public List<CommentResponse> getComments(Long ticketId, UserPrincipal currentUser) {
        // Also makes sure the current user is allowed to see this ticket.
        findAccessibleTicket(ticketId, currentUser);
        return commentRepository.findByTicketIdOrderByCreatedAtAsc(ticketId)
                .stream()
                .map(this::toCommentResponse)
                .toList();
    }

    /**
     * Loads the ticket and checks the current user may see it. Requesters only
     * get their own tickets; agents and admins can see everything.
     */
    private Ticket findAccessibleTicket(Long id, UserPrincipal currentUser) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found with id " + id));

        if (currentUser.getRole() == Role.REQUESTER
                && !ticket.getRequester().getId().equals(currentUser.getId())) {
            throw new ForbiddenException("You are not allowed to access this ticket");
        }
        return ticket;
    }

    private TicketResponse toResponse(Ticket ticket) {
        TicketResponse response = new TicketResponse();
        response.setId(ticket.getId());
        response.setTitle(ticket.getTitle());
        response.setDescription(ticket.getDescription());
        response.setPriority(ticket.getPriority());
        response.setStatus(ticket.getStatus());
        response.setRequesterId(ticket.getRequester().getId());
        response.setRequesterName(ticket.getRequester().getName());
        if (ticket.getAssignee() != null) {
            response.setAssigneeId(ticket.getAssignee().getId());
            response.setAssigneeName(ticket.getAssignee().getName());
        }
        response.setCreatedAt(ticket.getCreatedAt());
        response.setUpdatedAt(ticket.getUpdatedAt());
        return response;
    }

    private CommentResponse toCommentResponse(Comment comment) {
        CommentResponse response = new CommentResponse();
        response.setId(comment.getId());
        response.setTicketId(comment.getTicket().getId());
        response.setAuthorId(comment.getAuthor().getId());
        response.setAuthorName(comment.getAuthor().getName());
        response.setMessage(comment.getMessage());
        response.setCreatedAt(comment.getCreatedAt());
        return response;
    }
}
