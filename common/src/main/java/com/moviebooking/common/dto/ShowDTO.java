package com.moviebooking.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShowDTO {
    private Long showId;
    private Long movieId;
    private String movieTitle;
    private Long theatreId;
    private String theatreName;
    private String screenName;
    private LocalDate showDate;
    private LocalTime showTime;
    private Integer availableSeats;
    private BigDecimal basePrice;
    private String language;
    private String genre;
    private String city;
}
