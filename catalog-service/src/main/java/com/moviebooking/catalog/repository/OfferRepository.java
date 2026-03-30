package com.moviebooking.catalog.repository;

import com.moviebooking.catalog.model.Offer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface OfferRepository extends JpaRepository<Offer, Long> {
    
    @Query("SELECT o FROM Offer o " +
           "WHERE o.status = 'ACTIVE' " +
           "AND :date BETWEEN o.validFrom AND o.validTo")
    List<Offer> findActiveOffers(@Param("date") LocalDate date);
}
