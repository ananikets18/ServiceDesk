package com.servicedesk.repository;

import com.servicedesk.entity.Status;
import com.servicedesk.entity.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Database access for tickets. The extra finders follow Spring Data's
 * method-name convention, so no queries have to be written by hand.
 */
public interface TicketRepository extends JpaRepository<Ticket, Long> {

    List<Ticket> findByRequesterId(Long requesterId);

    List<Ticket> findByAssigneeId(Long assigneeId);

    List<Ticket> findByStatus(Status status);
}
