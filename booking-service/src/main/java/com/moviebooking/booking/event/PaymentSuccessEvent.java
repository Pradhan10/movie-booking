package com.moviebooking.booking.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentSuccessEvent implements Serializable {
    private Long paymentId;
    private Long bookingId;
    private BigDecimal amount;
    private String gatewayTxnId;
    private Instant completedAt;
}
