package com.moviebooking.catalog.model;

import com.moviebooking.common.enums.ShowStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "show", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"screen_id", "show_date", "show_time"})
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Show {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "movie_id", nullable = false)
    private Long movieId;
    
    @Column(name = "theatre_id", nullable = false)
    private Long theatreId;
    
    @Column(name = "screen_id", nullable = false)
    private Long screenId;
    
    @Column(name = "show_date", nullable = false)
    private LocalDate showDate;
    
    @Column(name = "show_time", nullable = false)
    private LocalTime showTime;
    
    @Column(name = "base_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal basePrice;
    
    @Column(name = "available_seats", nullable = false)
    private Integer availableSeats;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ShowStatus status;
    
    @Version
    @Column(nullable = false)
    private Integer version;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    
    @Column(name = "updated_at")
    private Instant updatedAt;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "movie_id", insertable = false, updatable = false)
    private Movie movie;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "theatre_id", insertable = false, updatable = false)
    private Theatre theatre;
    
    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
        if (status == null) {
            status = ShowStatus.ACTIVE;
        }
        if (version == null) {
            version = 0;
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
    
    public boolean isAvailable() {
        return status == ShowStatus.ACTIVE && availableSeats > 0;
    }
    
    public boolean isAfternoonShow() {
        return showTime.isAfter(LocalTime.of(12, 0)) && 
               showTime.isBefore(LocalTime.of(16, 0));
    }
}
