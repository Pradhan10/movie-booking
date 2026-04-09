package com.moviebooking.booking.dto;

import com.moviebooking.common.security.Sensitive;
import jakarta.validation.constraints.*;
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
    @Size(min = 10, max = 100, message = "Session ID must be between 10 and 100 characters")
    @Sensitive(Sensitive.MaskType.PARTIAL)
    private String sessionId;
    
    @NotNull(message = "Show ID is required")
    @Positive(message = "Show ID must be positive")
    private Long showId;
    
    @NotEmpty(message = "At least one seat must be selected")
    @Size(min = 1, max = 10, message = "Can hold between 1 and 10 seats per request")
    private List<@Positive(message = "Seat ID must be positive") Long> seatIds;
    
    @NotNull(message = "User ID is required")
    @Positive(message = "User ID must be positive")
    @Sensitive(Sensitive.MaskType.PARTIAL)
    private Long userId;
}
