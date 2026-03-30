package com.moviebooking.booking.model;

import com.moviebooking.common.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "payment")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Payment {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "booking_id", nullable = false)
    private Long bookingId;
    
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;
    
    @Column(name = "payment_method")
    private String paymentMethod;
    
    @Column(name = "gateway_name")
    private String gatewayName;
    
    @Column(name = "gateway_txn_id")
    private String gatewayTxnId;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;
    
    @Column(name = "gateway_response", columnDefinition = "TEXT")
    private String gatewayResponse;
    
    @Column(name = "initiated_at", nullable = false)
    private Instant initiatedAt;
    
    @Column(name = "completed_at")
    private Instant completedAt;
    
    @Column(name = "failed_reason", columnDefinition = "TEXT")
    private String failedReason;
    
    @PrePersist
    protected void onCreate() {
        if (initiatedAt == null) {
            initiatedAt = Instant.now();
        }
        if (status == null) {
            status = PaymentStatus.INITIATED;
        }
    }
    
    public void markSuccess(String txnId, String response) {
        this.status = PaymentStatus.SUCCESS;
        this.gatewayTxnId = txnId;
        this.gatewayResponse = response;
        this.completedAt = Instant.now();
    }
    
    public void markFailed(String reason) {
        this.status = PaymentStatus.FAILED;
        this.failedReason = reason;
        this.completedAt = Instant.now();
    }
}
