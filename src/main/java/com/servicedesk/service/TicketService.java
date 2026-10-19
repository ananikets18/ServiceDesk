package com.servicedesk.service;

import com.servicedesk.dto.AssignTicketRequest;
import com.servicedesk.dto.CommentRequest;
import com.servicedesk.dto.CommentResponse;
import com.servicedesk.dto.StatusUpdateRequest;
import com.servicedesk.dto.TicketRequest;
import com.servicedesk.dto.TicketResponse;
import com.servicedesk.entity.Comment;
import com.servicedesk.entity.Ticket;
import com.servicedesk.entity.User;
import com.servicedesk.exception.ResourceNotFoundException;
import com.servicedesk.repository.CommentRepository;
import com.servicedesk.repository.TicketRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * All the ticket business rules live here: creating them, reading them,
 * changing their status, assigning an agent and adding comments.
 * The controller only deals with HTTP; the repository only deals with the database.
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
    public TicketResponse createTicket(TicketRequest request) {
        User requester = userService.getById(request.getRequesterId());
        Ticket ticket = new Ticket(request.getTitle(), request.getDescription(), request.getPriority(), requester);
        Ticket saved = ticketRepository.save(ticket);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<TicketResponse> getAllTickets() {
        return ticketRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public TicketResponse getTicketById(Long id) {
        return toResponse(findTicket(id));
    }

    @Transactional
    public TicketResponse updateStatus(Long id, StatusUpdateRequest request) {
        Ticket ticket = findTicket(id);
        ticket.setStatus(request.getStatus());
        return toResponse(ticketRepository.save(ticket));
    }

    @Transactional
    public TicketResponse assignTicket(Long id, AssignTicketRequest request) {
        Ticket ticket = findTicket(id);
        User assignee = userService.getById(request.getAssigneeId());
        ticket.setAssignee(assignee);
        return toResponse(ticketRepository.save(ticket));
    }

    @Transactional
    public CommentResponse addComment(Long ticketId, CommentRequest request) {
        Ticket ticket = findTicket(ticketId);
        User author = userService.getById(request.getAuthorId());
        Comment comment = new Comment(ticket, author, request.getMessage());
        return toCommentResponse(commentRepository.save(comment));
    }

    @Transactional(readOnly = true)
    public List<CommentResponse> getComments(Long ticketId) {
        // Make sure the ticket exists before listing its comments.
        findTicket(ticketId);
        return commentRepository.findByTicketIdOrderByCreatedAtAsc(ticketId)
                .stream()
                .map(this::toCommentResponse)
                .toList();
    }

    private Ticket findTicket(Long id) {
        return ticketRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found with id " + id));
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
