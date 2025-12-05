//package com.helpdeskpro.backend.repository;
//
//import com.helpdeskpro.backend.entity.Comment;
//import com.helpdeskpro.backend.entity.Ticket;
//import org.springframework.data.jpa.repository.JpaRepository;
//import org.springframework.data.jpa.repository.EntityGraph; // <-- ADD THIS IMPORT
//import org.springframework.data.jpa.repository.Query;
//import org.springframework.data.repository.query.Param;
//import org.springframework.stereotype.Repository;
//
//import java.util.List;
//
///**
// * Comment Repository
// * Handles database operations for comments
// */
//@Repository
//public interface CommentRepository extends JpaRepository<Comment, Long> {
//
//    /**
//     * Find all non-deleted comments for a ticket
//     * Ordered by creation date (oldest first)
//     */
//    @Query("SELECT c FROM Comment c WHERE c.ticket.id = :ticketId AND c.deletedAt IS NULL ORDER BY c.createdAt ASC")
//    List<Comment> findByTicketIdOrderByCreatedAtAsc(@Param("ticketId") Long ticketId);
//
//    /**
//     * Find all comments by ticket (including deleted)
//     * Useful for admin audit purposes
//     */
//    List<Comment> findByTicket(Ticket ticket);
//
//    /**
//     * Count non-deleted comments for a ticket
//     */
//    @Query("SELECT COUNT(c) FROM Comment c WHERE c.ticket.id = :ticketId AND c.deletedAt IS NULL")
//    Long countByTicketId(@Param("ticketId") Long ticketId);
//
//    /**
//     * Find all non-deleted comments by a user
//     */
//    @Query("SELECT c FROM Comment c WHERE c.createdBy.id = :userId AND c.deletedAt IS NULL ORDER BY c.createdAt DESC")
//    List<Comment> findByCreatedByIdOrderByCreatedAtDesc(@Param("userId") Long userId);
//
//    // ADD THIS NEW METHOD TO FIX 500 ERRORS
//    @EntityGraph(attributePaths = {"createdBy"}) // Eagerly fetches the 'createdBy' user
//    Comment findWithCreatorById(Long id);
//}


package com.helpdeskpro.backend.repository;

import com.helpdeskpro.backend.entity.Comment;
import com.helpdeskpro.backend.entity.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Comment Repository
 * Handles database operations for comments
 */
@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    /**
     * Find all non-deleted comments for a ticket
     * Ordered by creation date (oldest first)
     */
    @Query("SELECT c FROM Comment c WHERE c.ticket.id = :ticketId AND c.deletedAt IS NULL ORDER BY c.createdAt ASC")
    List<Comment> findByTicketIdOrderByCreatedAtAsc(@Param("ticketId") Long ticketId);

    /**
     * Find all comments by ticket (including deleted)
     * Useful for admin audit purposes
     */
    List<Comment> findByTicket(Ticket ticket);

    /**
     * Count non-deleted comments for a ticket
     */
    @Query("SELECT COUNT(c) FROM Comment c WHERE c.ticket.id = :ticketId AND c.deletedAt IS NULL")
    Long countByTicketId(@Param("ticketId") Long ticketId);

    /**
     * Find all non-deleted comments by a user
     */
    @Query("SELECT c FROM Comment c WHERE c.createdBy.id = :userId AND c.deletedAt IS NULL ORDER BY c.createdAt DESC")
    List<Comment> findByCreatedByIdOrderByCreatedAtDesc(@Param("userId") Long userId);

    /**
     * Find comment by ID with creator eagerly loaded
     * Prevents LazyInitializationException when accessing createdBy
     */
    @EntityGraph(attributePaths = {"createdBy"})
    Optional<Comment> findWithCreatorById(Long id);
}