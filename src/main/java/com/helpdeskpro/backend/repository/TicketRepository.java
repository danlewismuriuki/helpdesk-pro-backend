package com.helpdeskpro.backend.repository;

import com.helpdeskpro.backend.entity.Ticket;
import com.helpdeskpro.backend.entity.User;
import com.helpdeskpro.backend.entity.enums.TicketStatus;
import com.helpdeskpro.backend.entity.enums.TicketPriority;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {
    Page<Ticket> findByStatus(TicketStatus status, Pageable pageable);
    Page<Ticket> findByPriority(TicketPriority priority, Pageable pageable);
    Page<Ticket> findByCreatedBy(User user, Pageable pageable);
    Page<Ticket> findByAssignedTo(User user, Pageable pageable);
    
    @Query("SELECT t FROM Ticket t WHERE t.slaDeadline < :now AND t.status NOT IN ('RESOLVED', 'CLOSED')")
    List<Ticket> findSlaBreachedTickets(LocalDateTime now);
    
    Long countByStatus(TicketStatus status);
    Long countByPriority(TicketPriority priority);
}
