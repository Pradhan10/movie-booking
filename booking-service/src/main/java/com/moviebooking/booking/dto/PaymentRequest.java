package com.moviebooking.booking.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRequest {
    
    @NotNull(message = "Booking ID is required")
    @Positive(message = "Booking ID must be positive")
    private Long bookingId;
    
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be at least 0.01")
    @DecimalMax(value = "100000.00", message = "Amount cannot exceed 100,000")
    @Digits(integer = 10, fraction = 2, message = "Amount must have at most 10 integer digits and 2 decimal places")
    private BigDecimal amount;
    
    @NotNull(message = "Payment method is required")
    @Pattern(regexp = "^(CARD|UPI|NET_BANKING|WALLET)$", message = "Payment method must be one of: CARD, UPI, NET_BANKING, WALLET")
    private String paymentMethod;
}
