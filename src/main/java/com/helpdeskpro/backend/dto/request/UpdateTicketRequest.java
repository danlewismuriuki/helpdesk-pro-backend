package com.helpdeskpro.backend.dto.request;

import com.helpdeskpro.backend.entity.enums.TicketPriority;
import com.helpdeskpro.backend.entity.enums.TicketStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateTicketRequest {
    private String title;
    private String description;
    private TicketStatus status;
    private TicketPriority priority;
    private Long assignedToId;
}
