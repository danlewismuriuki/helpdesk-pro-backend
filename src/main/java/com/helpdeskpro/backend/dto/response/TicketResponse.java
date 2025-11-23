package com.helpdeskpro.backend.dto.response;

import com.helpdeskpro.backend.entity.enums.TicketPriority;
import com.helpdeskpro.backend.entity.enums.TicketStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketResponse {
    private Long id;
    private String title;
    private String description;
    private TicketStatus status;
    private TicketPriority priority;
    private UserResponse createdBy;
    private UserResponse assignedTo;
    private LocalDateTime slaDeadline;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime resolvedAt;
    private Boolean isSlaBreached;
}
