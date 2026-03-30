package com.moviebooking.catalog.repository;

import com.moviebooking.catalog.model.Theatre;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TheatreRepository extends JpaRepository<Theatre, Long> {
    
    List<Theatre> findByCity(String city);
    
    List<Theatre> findByCityAndStatus(String city, String status);
}
