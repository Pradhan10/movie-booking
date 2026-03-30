package com.moviebooking.catalog.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "theatre")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Theatre {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String name;
    
    @Column(nullable = false)
    private String location;
    
    @Column(nullable = false)
    private String city;
    
    private String state;
    
    private String country;
    
    @Column(columnDefinition = "TEXT")
    private String address;
    
    @Column(name = "partner_id", nullable = false)
    private Long partnerId;
    
    @Column(name = "total_screens", nullable = false)
    private Integer totalScreens;
    
    @Column(nullable = false)
    private String status;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    
    @Column(name = "updated_at")
    private Instant updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
