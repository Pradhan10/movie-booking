package com.moviebooking.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeatDTO {
    private Long seatId;
    private String seatNumber;
    private String row;
    private String category;
    private BigDecimal price;
    private String status;
}
