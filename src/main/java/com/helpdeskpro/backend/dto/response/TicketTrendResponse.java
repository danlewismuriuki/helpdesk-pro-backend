package com.helpdeskpro.backend.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Ticket trend data for charts")
public class TicketTrendResponse {

    @Schema(description = "Date", example = "2024-12-01")
    private LocalDate date;

    @Schema(description = "Number of tickets created", example = "15")
    private Long created;

    @Schema(description = "Number of tickets resolved", example = "12")
    private Long resolved;

    @Schema(description = "Number of tickets closed", example = "10")
    private Long closed;
}