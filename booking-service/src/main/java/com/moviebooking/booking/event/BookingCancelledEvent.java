package com.moviebooking.booking.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingCancelledEvent implements Serializable {
    private Long bookingId;
    private String bookingReference;
    private Long userId;
    private String reason;
    private Instant cancelledAt;
}
