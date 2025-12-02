//package com.helpdeskpro.backend.repository;
//
//import com.helpdeskpro.backend.entity.Ticket;
//import com.helpdeskpro.backend.entity.User;
//import com.helpdeskpro.backend.entity.enums.TicketStatus;
//import com.helpdeskpro.backend.entity.enums.TicketPriority;
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.Pageable;
//import org.springframework.data.jpa.repository.JpaRepository;
//import org.springframework.data.jpa.repository.Query;
//import org.springframework.stereotype.Repository;
//
//import java.time.LocalDateTime;
//import java.util.List;
//
//@Repository
//public interface TicketRepository extends JpaRepository<Ticket, Long> {
//    Page<Ticket> findByStatus(TicketStatus status, Pageable pageable);
//    Page<Ticket> findByPriority(TicketPriority priority, Pageable pageable);
//    Page<Ticket> findByCreatedBy(User user, Pageable pageable);
//    Page<Ticket> findByAssignedTo(User user, Pageable pageable);
//
//    @Query("SELECT t FROM Ticket t WHERE t.slaDeadline < :now AND t.status NOT IN ('RESOLVED', 'CLOSED')")
//    List<Ticket> findSlaBreachedTickets(LocalDateTime now);
//
//    Long countByStatus(TicketStatus status);
//    Long countByPriority(TicketPriority priority);
//}

package com.helpdeskpro.backend.repository;

import com.helpdeskpro.backend.entity.Ticket;
import com.helpdeskpro.backend.entity.User;
import com.helpdeskpro.backend.entity.enums.TicketStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {

    /**
     * Find tickets by status with pagination
     */
    Page<Ticket> findByStatus(TicketStatus status, Pageable pageable);

    /**
     * Find tickets created by a specific user
     */
    Page<Ticket> findByCreatedBy(User user, Pageable pageable);

    /**
     * Find tickets assigned to a specific agent
     */
    Page<Ticket> findByAssignedTo(User agent, Pageable pageable);

    /**
     * Find tickets that have breached SLA deadline and are not resolved
     * Used for monitoring and reporting
     */
    List<Ticket> findBySlaDeadlineBeforeAndStatusNot(
            LocalDateTime deadline,
            TicketStatus status
    );

    /**
     * Find tickets by priority
     */
    Page<Ticket> findByPriority(String priority, Pageable pageable);

    /**
     * Count tickets by status (useful for admin dashboard)
     */
    Long countByStatus(TicketStatus status);

    /**
     * Find unassigned tickets
     */
    @Query("SELECT t FROM Ticket t WHERE t.assignedTo IS NULL AND t.deletedAt IS NULL")
    Page<Ticket> findUnassignedTickets(Pageable pageable);

    /**
     * Find tickets breaching SLA grouped by priority
     * Useful for prioritization dashboard
     */
    @Query("SELECT t FROM Ticket t WHERE t.slaDeadline < :now " +
            "AND t.status NOT IN ('RESOLVED', 'CLOSED') " +
            "AND t.deletedAt IS NULL " +
            "ORDER BY t.priority DESC, t.slaDeadline ASC")
    List<Ticket> findSlaBreachedTicketsOrderedByPriority(@Param("now") LocalDateTime now);
}