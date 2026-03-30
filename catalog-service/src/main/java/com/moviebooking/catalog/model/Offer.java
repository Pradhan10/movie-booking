package com.moviebooking.catalog.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "offer")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Offer {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false)
    private String code;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "discount_type", nullable = false)
    private String discountType;
    
    @Column(name = "discount_value", nullable = false, precision = 10, scale = 2)
    private BigDecimal discountValue;
    
    @Column(columnDefinition = "TEXT")
    private String conditions;
    
    @Column(name = "valid_from", nullable = false)
    private LocalDate validFrom;
    
    @Column(name = "valid_to", nullable = false)
    private LocalDate validTo;
    
    @Column(nullable = false)
    private String status;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }
    
    public boolean isValid(LocalDate date) {
        return status.equals("ACTIVE") && 
               !date.isBefore(validFrom) && 
               !date.isAfter(validTo);
    }
}
