package com.moviebooking.booking.repository;

import com.moviebooking.booking.model.Seat;
import com.moviebooking.common.enums.SeatStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface SeatRepository extends JpaRepository<Seat, Long> {
    
    List<Seat> findByShowIdAndStatus(Long showId, SeatStatus status);
    
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Seat s WHERE s.id IN :seatIds AND s.status = :status")
    List<Seat> findByIdsAndStatusForUpdate(
        @Param("seatIds") List<Long> seatIds,
        @Param("status") SeatStatus status
    );
    
    @Modifying
    @Query("UPDATE Seat s SET s.status = :newStatus, s.heldUntil = null, s.version = s.version + 1 " +
           "WHERE s.id = :seatId AND s.version = :expectedVersion AND s.status = :currentStatus")
    int updateStatusWithOptimisticLock(
        @Param("seatId") Long seatId,
        @Param("currentStatus") SeatStatus currentStatus,
        @Param("newStatus") SeatStatus newStatus,
        @Param("expectedVersion") Integer expectedVersion
    );
    
    @Query("SELECT s FROM Seat s WHERE s.status = 'HELD' AND s.heldUntil < :now")
    List<Seat> findExpiredHolds(@Param("now") Instant now);
    
    @Modifying
    @Query("UPDATE Seat s SET s.status = 'AVAILABLE', s.heldUntil = null WHERE s.status = 'HELD' AND s.heldUntil < :now")
    int releaseExpiredHolds(@Param("now") Instant now);
}
