package com.moviebooking.booking.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingConfirmedEvent implements Serializable {
    private Long bookingId;
    private String bookingReference;
    private Long userId;
    private Long showId;
    private List<Long> seatIds;
    private BigDecimal finalAmount;
    private Instant confirmedAt;
}
