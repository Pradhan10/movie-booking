package com.moviebooking.booking.repository;

import com.moviebooking.booking.model.Payment;
import com.moviebooking.common.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    
    Optional<Payment> findByBookingId(Long bookingId);
    
    Optional<Payment> findByGatewayTxnId(String gatewayTxnId);
    
    List<Payment> findByStatus(PaymentStatus status);
}
