package com.moviebooking.booking.service;

import com.moviebooking.booking.dto.BookingRequest;
import com.moviebooking.booking.dto.HoldSeatsRequest;
import com.moviebooking.booking.dto.HoldSeatsResponse;
import com.moviebooking.booking.event.BookingCancelledEvent;
import com.moviebooking.booking.event.BookingConfirmedEvent;
import com.moviebooking.booking.exception.HoldExpiredException;
import com.moviebooking.booking.exception.SeatUnavailableException;
import com.moviebooking.booking.model.Booking;
import com.moviebooking.booking.model.BookingSeat;
import com.moviebooking.booking.model.Seat;
import com.moviebooking.booking.repository.BookingRepository;
import com.moviebooking.booking.repository.SeatRepository;
import com.moviebooking.common.enums.BookingStatus;
import com.moviebooking.common.enums.SeatStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingService {
    
    private final BookingRepository bookingRepository;
    private final SeatRepository seatRepository;
    private final SeatLockService seatLockService;
    private final EventPublisher eventPublisher;
    
    @Value("${booking.seat-hold.ttl-minutes:10}")
    private int holdTtlMinutes;
    
    @Transactional
    public HoldSeatsResponse holdSeats(HoldSeatsRequest request) {
        String transactionId = UUID.randomUUID().toString();
        List<Long> seatIds = request.getSeatIds();
        
        log.info("Attempting to hold seats: {} for session: {}", seatIds, request.getSessionId());
        
        List<Seat> seatsToHold = seatRepository.findByIdsAndStatusForUpdate(seatIds, SeatStatus.AVAILABLE);
        
        if (seatsToHold.size() != seatIds.size()) {
            Set<Long> foundIds = seatsToHold.stream().map(Seat::getId).collect(Collectors.toSet());
            List<Long> unavailableIds = seatIds.stream()
                .filter(id -> !foundIds.contains(id))
                .collect(Collectors.toList());
            throw new SeatUnavailableException("Seats not available: " + unavailableIds);
        }
        
        for (Long seatId : seatIds) {
            if (!seatLockService.acquireLock(seatId, transactionId)) {
                seatIds.forEach(id -> seatLockService.releaseLock(id, transactionId));
                throw new SeatUnavailableException("Failed to acquire lock for seat: " + seatId);
            }
        }
        
        Instant holdUntil = Instant.now().plus(holdTtlMinutes, ChronoUnit.MINUTES);
        
        try {
            for (Seat seat : seatsToHold) {
                seat.hold(holdUntil);
            }
            seatRepository.saveAll(seatsToHold);
            
            seatLockService.holdSeats(request.getSessionId(), seatIds, request.getUserId(), holdUntil);
            
            log.info("Successfully held {} seats for session: {}", seatsToHold.size(), request.getSessionId());
            
            return HoldSeatsResponse.builder()
                .sessionId(request.getSessionId())
                .seatIds(seatIds)
                .expiresAt(holdUntil)
                .success(true)
                .build();
        } finally {
            seatIds.forEach(id -> seatLockService.releaseLock(id, transactionId));
        }
    }
    
    @Transactional
    public Booking createBooking(BookingRequest request) {
        log.info("Creating booking for user: {}, show: {}, seats: {}", 
            request.getUserId(), request.getShowId(), request.getSeatIds());
        
        Set<Long> heldSeats = seatLockService.getHeldSeats(request.getSessionId());
        if (!heldSeats.containsAll(request.getSeatIds())) {
            throw new HoldExpiredException("Seat hold expired or invalid session");
        }
        
        List<Seat> seats = seatRepository.findAllById(request.getSeatIds());
        
        for (Seat seat : seats) {
            if (seat.isHoldExpired()) {
                throw new HoldExpiredException("Seat hold expired for seat: " + seat.getSeatNumber());
            }
            if (!seat.isHeld()) {
                throw new SeatUnavailableException("Seat not held: " + seat.getSeatNumber());
            }
        }
        
        BigDecimal totalAmount = seats.stream()
            .map(seat -> BigDecimal.valueOf(250.00))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal discountAmount = BigDecimal.ZERO;
        
        BigDecimal finalAmount = totalAmount.subtract(discountAmount);
        
        String bookingReference = generateBookingReference();
        
        Booking booking = Booking.builder()
            .bookingReference(bookingReference)
            .userId(request.getUserId())
            .showId(request.getShowId())
            .totalAmount(totalAmount)
            .discountAmount(discountAmount)
            .finalAmount(finalAmount)
            .status(BookingStatus.PENDING)
            .build();
        
        for (Seat seat : seats) {
            BookingSeat bookingSeat = BookingSeat.builder()
                .seatId(seat.getId())
                .pricePaid(BigDecimal.valueOf(250.00))
                .build();
            booking.addSeat(bookingSeat);
        }
        
        booking = bookingRepository.save(booking);
        
        log.info("Booking created: {} with reference: {}", booking.getId(), bookingReference);
        
        return booking;
    }
    
    @Transactional
    public Booking confirmBooking(Long bookingId, Long paymentId) {
        log.info("Confirming booking: {} with payment: {}", bookingId, paymentId);
        
        Booking booking = bookingRepository.findById(bookingId)
            .orElseThrow(() -> new RuntimeException("Booking not found: " + bookingId));
        
        if (booking.getStatus() != BookingStatus.PENDING) {
            log.warn("Booking {} already in status: {}", bookingId, booking.getStatus());
            return booking;
        }
        
        List<Seat> seats = seatRepository.findAllById(
            booking.getSeats().stream()
                .map(BookingSeat::getSeatId)
                .collect(Collectors.toList())
        );
        
        String transactionId = UUID.randomUUID().toString();
        
        try {
            for (Seat seat : seats) {
                if (!seatLockService.acquireLock(seat.getId(), transactionId)) {
                    throw new SeatUnavailableException("Failed to acquire lock for final booking");
                }
            }
            
            for (Seat seat : seats) {
                int updated = seatRepository.updateStatusWithOptimisticLock(
                    seat.getId(), 
                    SeatStatus.HELD, 
                    SeatStatus.BOOKED, 
                    seat.getVersion()
                );
                
                if (updated == 0) {
                    throw new ObjectOptimisticLockingFailureException(
                        Seat.class, seat.getId()
                    );
                }
            }
            
            booking.setPaymentId(paymentId);
            booking.confirm();
            booking = bookingRepository.save(booking);
            
            BookingConfirmedEvent event = BookingConfirmedEvent.builder()
                .bookingId(booking.getId())
                .bookingReference(booking.getBookingReference())
                .userId(booking.getUserId())
                .showId(booking.getShowId())
                .seatIds(seats.stream().map(Seat::getId).collect(Collectors.toList()))
                .finalAmount(booking.getFinalAmount())
                .confirmedAt(Instant.now())
                .build();
            
            eventPublisher.publishBookingConfirmed(event);
            
            log.info("Booking confirmed: {}", bookingId);
            
            return booking;
        } finally {
            seats.forEach(seat -> seatLockService.releaseLock(seat.getId(), transactionId));
        }
    }
    
    @Transactional
    public void cancelBooking(Long bookingId, String reason) {
        log.info("Cancelling booking: {} - reason: {}", bookingId, reason);
        
        Booking booking = bookingRepository.findById(bookingId)
            .orElseThrow(() -> new RuntimeException("Booking not found: " + bookingId));
        
        if (booking.getStatus() == BookingStatus.CONFIRMED) {
            List<Long> seatIds = booking.getSeats().stream()
                .map(BookingSeat::getSeatId)
                .collect(Collectors.toList());
            
            List<Seat> seats = seatRepository.findAllById(seatIds);
            seats.forEach(Seat::release);
            seatRepository.saveAll(seats);
        }
        
        booking.cancel();
        bookingRepository.save(booking);
        
        BookingCancelledEvent event = BookingCancelledEvent.builder()
            .bookingId(booking.getId())
            .bookingReference(booking.getBookingReference())
            .userId(booking.getUserId())
            .reason(reason)
            .cancelledAt(Instant.now())
            .build();
        
        eventPublisher.publishBookingCancelled(event);
        
        log.info("Booking cancelled: {}", bookingId);
    }
    
    private String generateBookingReference() {
        return "BK" + Instant.now().toEpochMilli() + "-" + 
               UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
