package com.moviebooking.booking.model;

import com.moviebooking.common.enums.SeatCategory;
import com.moviebooking.common.enums.SeatStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "seat", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"show_id", "seat_number"})
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Seat {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "show_id", nullable = false)
    private Long showId;
    
    @Column(name = "seat_number", nullable = false)
    private String seatNumber;
    
    @Column(name = "row_label", nullable = false)
    private String rowLabel;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SeatCategory category;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SeatStatus status;
    
    @Version
    @Column(nullable = false)
    private Integer version;
    
    @Column(name = "held_until")
    private Instant heldUntil;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    
    @Column(name = "updated_at")
    private Instant updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
        if (status == null) {
            status = SeatStatus.AVAILABLE;
        }
        if (version == null) {
            version = 0;
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
    
    public void hold(Instant until) {
        this.status = SeatStatus.HELD;
        this.heldUntil = until;
    }
    
    public void book() {
        this.status = SeatStatus.BOOKED;
        this.heldUntil = null;
    }
    
    public void release() {
        this.status = SeatStatus.AVAILABLE;
        this.heldUntil = null;
    }
    
    public boolean isAvailable() {
        return status == SeatStatus.AVAILABLE;
    }
    
    public boolean isHeld() {
        return status == SeatStatus.HELD;
    }
    
    public boolean isHoldExpired() {
        return isHeld() && heldUntil != null && Instant.now().isAfter(heldUntil);
    }
}
