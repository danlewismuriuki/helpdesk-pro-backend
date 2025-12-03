package com.helpdeskpro.backend.service;

import com.helpdeskpro.backend.dto.response.*;
import com.helpdeskpro.backend.entity.Article;
import com.helpdeskpro.backend.entity.Ticket;
import com.helpdeskpro.backend.entity.User;
import com.helpdeskpro.backend.entity.enums.TicketPriority;
import com.helpdeskpro.backend.entity.enums.TicketStatus;
import com.helpdeskpro.backend.entity.enums.UserRole;
import com.helpdeskpro.backend.repository.ArticleRepository;
import com.helpdeskpro.backend.repository.TicketRepository;
import com.helpdeskpro.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Admin Statistics Service
 * Provides dashboard analytics and metrics
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AdminStatsService {

    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final ArticleRepository articleRepository;

    /**
     * Get complete dashboard statistics
     */
    public DashboardStatsResponse getDashboardStats() {
        log.info("Generating dashboard statistics");

        return DashboardStatsResponse.builder()
                .totalTickets(getTotalTickets())
                .ticketsByStatus(getTicketsByStatus())
                .ticketsByPriority(getTicketsByPriority())
                .averageResolutionTimeHours(getAverageResolutionTime())
                .slaBreachRate(getSlaBreachRate())
                .slaBreachedTickets(getSlaBreachedCount())
                .topAgents(getTopAgents(5))
                .knowledgeBaseStats(getKnowledgeBaseStats())
                .recentActivity(getRecentActivity(10))
                .build();
    }

    /**
     * Get total ticket count
     */
    private Long getTotalTickets() {
        return ticketRepository.count();
    }

    /**
     * Get tickets grouped by status
     */
    private Map<String, Long> getTicketsByStatus() {
        Map<String, Long> stats = new HashMap<>();

        for (TicketStatus status : TicketStatus.values()) {
            Long count = ticketRepository.countByStatus(status);
            stats.put(status.name(), count);
        }

        return stats;
    }

    /**
     * Get tickets grouped by priority
     */
    private Map<String, Long> getTicketsByPriority() {
        Map<String, Long> stats = new HashMap<>();

        List<Ticket> allTickets = ticketRepository.findAll();

        for (TicketPriority priority : TicketPriority.values()) {
            long count = allTickets.stream()
                    .filter(t -> t.getPriority() == priority)
                    .count();
            stats.put(priority.name(), count);
        }

        return stats;
    }

    /**
     * Calculate average ticket resolution time in hours
     */
    private Double getAverageResolutionTime() {
        List<Ticket> resolvedTickets = ticketRepository.findAll().stream()
                .filter(t -> t.getStatus() == TicketStatus.RESOLVED && t.getResolvedAt() != null)
                .toList();

        if (resolvedTickets.isEmpty()) {
            return 0.0;
        }

        double totalHours = resolvedTickets.stream()
                .mapToDouble(ticket -> {
                    Duration duration = Duration.between(ticket.getCreatedAt(), ticket.getResolvedAt());
                    return duration.toHours();
                })
                .sum();

        return totalHours / resolvedTickets.size();
    }

    /**
     * Calculate SLA breach rate percentage
     */
    private Double getSlaBreachRate() {
        Long total = getTotalTickets();
        if (total == 0) {
            return 0.0;
        }

        Long breached = getSlaBreachedCount();
        return (breached.doubleValue() / total.doubleValue()) * 100;
    }

    /**
     * Get count of tickets breaching SLA
     */
    private Long getSlaBreachedCount() {
        LocalDateTime now = LocalDateTime.now();

        return ticketRepository.findAll().stream()
                .filter(t -> t.getSlaDeadline().isBefore(now))
                .filter(t -> t.getStatus() != TicketStatus.RESOLVED && t.getStatus() != TicketStatus.CLOSED)
                .count();
    }

    /**
     * Get top performing agents by resolved ticket count
     */
    private List<AgentPerformanceResponse> getTopAgents(int limit) {
        List<User> agents = userRepository.findByRole(UserRole.AGENT);

        return agents.stream()
                .map(agent -> {
                    List<Ticket> resolvedTickets = ticketRepository.findAll().stream()
                            .filter(t -> t.getAssignedTo() != null)
                            .filter(t -> t.getAssignedTo().getId().equals(agent.getId()))
                            .filter(t -> t.getStatus() == TicketStatus.RESOLVED)
                            .toList();

                    List<Ticket> assignedTickets = ticketRepository.findAll().stream()
                            .filter(t -> t.getAssignedTo() != null)
                            .filter(t -> t.getAssignedTo().getId().equals(agent.getId()))
                            .filter(t -> t.getStatus() != TicketStatus.RESOLVED && t.getStatus() != TicketStatus.CLOSED)
                            .toList();

                    // Calculate average resolution time for this agent
                    double avgResolutionTime = 0.0;
                    if (!resolvedTickets.isEmpty()) {
                        double totalHours = resolvedTickets.stream()
                                .filter(t -> t.getResolvedAt() != null)
                                .mapToDouble(ticket -> {
                                    Duration duration = Duration.between(ticket.getCreatedAt(), ticket.getResolvedAt());
                                    return duration.toHours();
                                })
                                .sum();
                        avgResolutionTime = totalHours / resolvedTickets.size();
                    }

                    return AgentPerformanceResponse.builder()
                            .agentId(agent.getId())
                            .agentName(agent.getFirstName() + " " + agent.getLastName())
                            .agentEmail(agent.getEmail())
                            .resolvedCount((long) resolvedTickets.size())
                            .assignedCount((long) assignedTickets.size())
                            .averageResolutionTime(avgResolutionTime)
                            .build();
                })
                .sorted((a1, a2) -> Long.compare(a2.getResolvedCount(), a1.getResolvedCount()))
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * Get knowledge base statistics
     */
    private KnowledgeBaseStatsResponse getKnowledgeBaseStats() {
        Long totalPublished = articleRepository.countPublished();

        Long totalDrafts = articleRepository.findAll().stream()
                .filter(a -> !a.getPublished() && a.getDeletedAt() == null)
                .count();

        List<Article> publishedArticles = articleRepository.findAll().stream()
                .filter(Article::getPublished)
                .filter(a -> a.getDeletedAt() == null)
                .toList();

        Long totalViews = publishedArticles.stream()
                .mapToLong(Article::getViewCount)
                .sum();

        // Calculate average helpfulness
        Double avgHelpfulness = publishedArticles.stream()
                .mapToDouble(Article::getHelpfulnessPercentage)
                .average()
                .orElse(0.0);

        // Get most viewed article
        ArticleSummaryResponse mostViewed = publishedArticles.stream()
                .max(Comparator.comparing(Article::getViewCount))
                .map(this::mapToSummary)
                .orElse(null);

        return KnowledgeBaseStatsResponse.builder()
                .totalArticles(totalPublished)
                .draftArticles(totalDrafts)
                .totalViews(totalViews)
                .averageHelpfulness(avgHelpfulness)
                .mostViewedArticle(mostViewed)
                .build();
    }

    /**
     * Get recent activity feed
     */
    private List<RecentActivityResponse> getRecentActivity(int limit) {
        List<RecentActivityResponse> activities = new ArrayList<>();

        // Get recent tickets
        List<Ticket> recentTickets = ticketRepository.findAll().stream()
                .sorted((t1, t2) -> t2.getCreatedAt().compareTo(t1.getCreatedAt()))
                .limit(limit)
                .toList();

        for (Ticket ticket : recentTickets) {
            activities.add(RecentActivityResponse.builder()
                    .activityType("TICKET_CREATED")
                    .description("New ticket: " + ticket.getTitle())
                    .userName(ticket.getCreatedBy().getFirstName() + " " + ticket.getCreatedBy().getLastName())
                    .entityId(ticket.getId())
                    .entityType("TICKET")
                    .timestamp(ticket.getCreatedAt())
                    .build());

            // Add resolved activity if resolved
            if (ticket.getResolvedAt() != null) {
                activities.add(RecentActivityResponse.builder()
                        .activityType("TICKET_RESOLVED")
                        .description("Ticket resolved: " + ticket.getTitle())
                        .userName(ticket.getAssignedTo() != null ?
                                ticket.getAssignedTo().getFirstName() + " " + ticket.getAssignedTo().getLastName() :
                                "System")
                        .entityId(ticket.getId())
                        .entityType("TICKET")
                        .timestamp(ticket.getResolvedAt())
                        .build());
            }
        }

        // Sort by timestamp descending and limit
        return activities.stream()
                .sorted((a1, a2) -> a2.getTimestamp().compareTo(a1.getTimestamp()))
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * Map Article to ArticleSummaryResponse
     */
    private ArticleSummaryResponse mapToSummary(Article article) {
        return ArticleSummaryResponse.builder()
                .id(article.getId())
                .title(article.getTitle())
                .slug(article.getSlug())
                .summary(article.getSummary())
                .categoryName(article.getCategory() != null ? article.getCategory().getName() : null)
                .tags(article.getTags())
                .viewCount(article.getViewCount())
                .helpfulnessPercentage(article.getHelpfulnessPercentage())
                .publishedAt(article.getPublishedAt())
                .build();
    }
}