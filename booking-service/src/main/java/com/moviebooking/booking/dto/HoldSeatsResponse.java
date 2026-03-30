package com.moviebooking.booking.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HoldSeatsResponse {
    private String sessionId;
    private List<Long> seatIds;
    private Instant expiresAt;
    private boolean success;
    private String message;
}
