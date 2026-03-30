package com.moviebooking.catalog.service;

import com.moviebooking.catalog.model.Offer;
import com.moviebooking.catalog.repository.OfferRepository;
import com.moviebooking.common.dto.OfferDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OfferService {
    
    private final OfferRepository offerRepository;
    
    public List<OfferDTO> getApplicableOffers(String city, Long theatreId) {
        LocalDate today = LocalDate.now();
        List<Offer> offers = offerRepository.findActiveOffers(today);
        
        return offers.stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }
    
    public BigDecimal calculateDiscount(List<BigDecimal> seatPrices, LocalTime showTime, String city) {
        BigDecimal totalDiscount = BigDecimal.ZERO;
        
        totalDiscount = totalDiscount.add(calculateThirdTicketDiscount(seatPrices));
        
        totalDiscount = totalDiscount.add(calculateAfternoonDiscount(seatPrices, showTime));
        
        return totalDiscount;
    }
    
    private BigDecimal calculateThirdTicketDiscount(List<BigDecimal> seatPrices) {
        if (seatPrices.size() >= 3) {
            seatPrices = new ArrayList<>(seatPrices);
            seatPrices.sort(BigDecimal::compareTo);
            
            BigDecimal thirdTicketPrice = seatPrices.get(2);
            BigDecimal discount = thirdTicketPrice.multiply(BigDecimal.valueOf(0.50));
            
            log.info("Third ticket discount applied: {} (50% of {})", discount, thirdTicketPrice);
            return discount;
        }
        return BigDecimal.ZERO;
    }
    
    private BigDecimal calculateAfternoonDiscount(List<BigDecimal> seatPrices, LocalTime showTime) {
        if (isAfternoonShow(showTime)) {
            BigDecimal totalPrice = seatPrices.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            BigDecimal discount = totalPrice.multiply(BigDecimal.valueOf(0.20));
            
            log.info("Afternoon show discount applied: {} (20% of {})", discount, totalPrice);
            return discount;
        }
        return BigDecimal.ZERO;
    }
    
    private boolean isAfternoonShow(LocalTime showTime) {
        return showTime.isAfter(LocalTime.of(12, 0)) && 
               showTime.isBefore(LocalTime.of(16, 0));
    }
    
    private OfferDTO convertToDTO(Offer offer) {
        return OfferDTO.builder()
            .offerId(offer.getId())
            .code(offer.getCode())
            .description(offer.getDescription())
            .discountType(offer.getDiscountType())
            .discountValue(offer.getDiscountValue())
            .conditions(offer.getConditions())
            .build();
    }
}
