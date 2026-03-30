package com.moviebooking.booking.dto;

import com.moviebooking.common.dto.SeatDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingResponse {
    private Long bookingId;
    private String bookingReference;
    private String status;
    private BigDecimal totalAmount;
    private BigDecimal discountAmount;
    private BigDecimal finalAmount;
    private List<SeatDTO> seats;
    private String paymentUrl;
    private Long paymentId;
    private Instant expiresAt;
    private String qrCode;
}
