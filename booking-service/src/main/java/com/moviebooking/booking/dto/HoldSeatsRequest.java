package com.moviebooking.booking.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HoldSeatsRequest {
    
    @NotNull(message = "Session ID is required")
    private String sessionId;
    
    @NotNull(message = "Show ID is required")
    private Long showId;
    
    @NotEmpty(message = "At least one seat must be selected")
    private List<Long> seatIds;
    
    @NotNull(message = "User ID is required")
    private Long userId;
}
