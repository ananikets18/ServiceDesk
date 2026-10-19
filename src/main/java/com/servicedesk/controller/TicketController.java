package com.servicedesk.controller;

import com.servicedesk.dto.AssignTicketRequest;
import com.servicedesk.dto.CommentRequest;
import com.servicedesk.dto.CommentResponse;
import com.servicedesk.dto.StatusUpdateRequest;
import com.servicedesk.dto.TicketRequest;
import com.servicedesk.dto.TicketResponse;
import com.servicedesk.security.UserPrincipal;
import com.servicedesk.service.TicketService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * The ticket endpoints. These just receive the request, hand it to the service
 * and return the result - the real logic lives in TicketService.
 * The logged-in user is injected through @AuthenticationPrincipal.
 */
@RestController
@RequestMapping("/api/v1/tickets")
public class TicketController {

    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @PostMapping
    public ResponseEntity<TicketResponse> createTicket(@Valid @RequestBody TicketRequest request,
                                                       @AuthenticationPrincipal UserPrincipal currentUser) {
        TicketResponse created = ticketService.createTicket(request, currentUser);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @GetMapping
    public List<TicketResponse> getAllTickets(@AuthenticationPrincipal UserPrincipal currentUser) {
        return ticketService.getAllTickets(currentUser);
    }

    @GetMapping("/{id}")
    public TicketResponse getTicket(@PathVariable Long id,
                                    @AuthenticationPrincipal UserPrincipal currentUser) {
        return ticketService.getTicketById(id, currentUser);
    }

    @PatchMapping("/{id}/status")
    public TicketResponse updateStatus(@PathVariable Long id,
                                       @Valid @RequestBody StatusUpdateRequest request,
                                       @AuthenticationPrincipal UserPrincipal currentUser) {
        return ticketService.updateStatus(id, request, currentUser);
    }

    @PatchMapping("/{id}/assign")
    public TicketResponse assignTicket(@PathVariable Long id,
                                       @Valid @RequestBody AssignTicketRequest request,
                                       @AuthenticationPrincipal UserPrincipal currentUser) {
        return ticketService.assignTicket(id, request, currentUser);
    }

    @PostMapping("/{id}/comments")
    public ResponseEntity<CommentResponse> addComment(@PathVariable Long id,
                                                      @Valid @RequestBody CommentRequest request,
                                                      @AuthenticationPrincipal UserPrincipal currentUser) {
        CommentResponse created = ticketService.addComment(id, request, currentUser);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @GetMapping("/{id}/comments")
    public List<CommentResponse> getComments(@PathVariable Long id,
                                             @AuthenticationPrincipal UserPrincipal currentUser) {
        return ticketService.getComments(id, currentUser);
    }
}
