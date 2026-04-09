package com.moviebooking.booking.dto;

import com.moviebooking.common.security.Sensitive;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingRequest {
    
    @NotNull(message = "User ID is required")
    @Positive(message = "User ID must be positive")
    @Sensitive(Sensitive.MaskType.PARTIAL)
    private Long userId;
    
    @NotNull(message = "Show ID is required")
    @Positive(message = "Show ID must be positive")
    private Long showId;
    
    @NotEmpty(message = "At least one seat must be selected")
    @Size(min = 1, max = 10, message = "Can book between 1 and 10 seats per transaction")
    private List<@Positive(message = "Seat ID must be positive") Long> seatIds;
    
    @NotNull(message = "Session ID is required")
    @Size(min = 10, max = 100, message = "Session ID must be between 10 and 100 characters")
    @Sensitive(Sensitive.MaskType.PARTIAL)
    private String sessionId;
    
    @Size(max = 50, message = "Offer code cannot exceed 50 characters")
    @Pattern(regexp = "^[A-Z0-9_-]*$", message = "Offer code must contain only uppercase letters, numbers, hyphens, and underscores")
    private String offerCode;
    
    @Email(message = "Invalid email format")
    @Size(max = 100, message = "Email cannot exceed 100 characters")
    @Sensitive(Sensitive.MaskType.PARTIAL)
    private String contactEmail;
    
    @Pattern(regexp = "^\\+?[0-9]{10,15}$", message = "Phone must be 10-15 digits, optionally starting with +")
    @Sensitive(Sensitive.MaskType.PARTIAL)
    private String contactPhone;
}
