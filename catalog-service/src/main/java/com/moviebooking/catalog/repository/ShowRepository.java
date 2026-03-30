package com.moviebooking.catalog.repository;

import com.moviebooking.catalog.model.Show;
import com.moviebooking.common.enums.ShowStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ShowRepository extends JpaRepository<Show, Long> {
    
    @Query("SELECT s FROM Show s " +
           "JOIN FETCH s.movie m " +
           "JOIN FETCH s.theatre t " +
           "WHERE s.movieId = :movieId " +
           "AND s.showDate = :date " +
           "AND s.status = :status " +
           "AND t.city = :city " +
           "ORDER BY s.showTime")
    List<Show> findByMovieAndDateAndCity(
        @Param("movieId") Long movieId,
        @Param("date") LocalDate date,
        @Param("city") String city,
        @Param("status") ShowStatus status
    );
    
    @Query("SELECT s FROM Show s " +
           "JOIN FETCH s.movie m " +
           "JOIN FETCH s.theatre t " +
           "WHERE s.theatreId = :theatreId " +
           "AND s.showDate = :date " +
           "AND s.status = :status " +
           "ORDER BY s.showTime")
    List<Show> findByTheatreAndDate(
        @Param("theatreId") Long theatreId,
        @Param("date") LocalDate date,
        @Param("status") ShowStatus status
    );
    
    @Query("SELECT s FROM Show s " +
           "JOIN FETCH s.movie m " +
           "JOIN FETCH s.theatre t " +
           "WHERE s.showDate >= :startDate " +
           "AND s.showDate <= :endDate " +
           "AND s.status = :status " +
           "AND t.city = :city " +
           "ORDER BY s.showDate, s.showTime")
    List<Show> findByCityAndDateRange(
        @Param("city") String city,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate,
        @Param("status") ShowStatus status
    );
}
