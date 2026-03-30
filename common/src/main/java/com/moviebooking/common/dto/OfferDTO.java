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
public class OfferDTO {
    private Long offerId;
    private String code;
    private String description;
    private String discountType;
    private BigDecimal discountValue;
    private String conditions;
}
