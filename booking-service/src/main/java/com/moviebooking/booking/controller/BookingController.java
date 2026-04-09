package com.moviebooking.booking.controller;

import com.moviebooking.booking.dto.*;
import com.moviebooking.booking.model.Booking;
import com.moviebooking.booking.service.BookingService;
import com.moviebooking.booking.service.PaymentService;
import com.moviebooking.common.dto.SeatDTO;
import com.moviebooking.common.logging.CorrelationIdHolder;
import com.moviebooking.common.security.DataMaskingUtil;
import com.moviebooking.common.security.SecurityUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.stream.Collectors;

@RestController
@RequestMapping("/bookings")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Booking", description = "APIs for movie ticket booking with seat locking")
@SecurityRequirement(name = "bearerAuth")
public class BookingController {
    
    private final BookingService bookingService;
    private final PaymentService paymentService;
    
    @PostMapping("/hold")
    @Operation(summary = "Hold seats", description = "Temporarily hold seats for 10 minutes before booking")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<HoldSeatsResponse> holdSeats(@Valid @RequestBody HoldSeatsRequest request) {
        String correlationId = CorrelationIdHolder.get();
        Long authenticatedUserId = SecurityUtil.getCurrentUserId();
        
        // Verify request userId matches authenticated user
        if (!request.getUserId().equals(authenticatedUserId)) {
            log.warn("[{}] User {} attempted to hold seats for user {}", 
                correlationId, authenticatedUserId, request.getUserId());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        log.info("[{}] POST /bookings/hold - user: {}, show: {}, seats: {}", 
                correlationId, 
                DataMaskingUtil.maskUserId(authenticatedUserId), 
                request.getShowId(), 
                request.getSeatIds());
        
        HoldSeatsResponse response = bookingService.holdSeats(request);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping
    @Operation(summary = "Create booking", description = "Create a booking for held seats")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BookingResponse> createBooking(@Valid @RequestBody BookingRequest request) {
        String correlationId = CorrelationIdHolder.get();
        Long authenticatedUserId = SecurityUtil.getCurrentUserId();
        
        // Verify request userId matches authenticated user
        if (!request.getUserId().equals(authenticatedUserId)) {
            log.warn("[{}] User {} attempted to create booking for user {}", 
                correlationId, authenticatedUserId, request.getUserId());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        log.info("[{}] POST /bookings - user: {}, show: {}, seats: {}", 
                correlationId,
                DataMaskingUtil.maskUserId(authenticatedUserId), 
                request.getShowId(), 
                request.getSeatIds());
        
        Booking booking = bookingService.createBooking(request);
        
        PaymentRequest paymentRequest = PaymentRequest.builder()
            .bookingId(booking.getId())
            .amount(booking.getFinalAmount())
            .paymentMethod("CARD")
            .build();
        
        PaymentResponse paymentResponse = paymentService.initiatePayment(paymentRequest);
        
        BookingResponse response = BookingResponse.builder()
            .bookingId(booking.getId())
            .bookingReference(booking.getBookingReference())
            .status(booking.getStatus().name())
            .totalAmount(booking.getTotalAmount())
            .discountAmount(booking.getDiscountAmount())
            .finalAmount(booking.getFinalAmount())
            .paymentUrl(paymentResponse.getPaymentUrl())
            .paymentId(paymentResponse.getPaymentId())
            .seats(booking.getSeats().stream()
                .map(bs -> SeatDTO.builder()
                    .seatId(bs.getSeatId())
                    .price(bs.getPricePaid())
                    .build())
                .collect(Collectors.toList()))
            .build();
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @PostMapping("/{bookingId}/confirm")
    @Operation(summary = "Confirm booking", description = "Confirm booking after successful payment")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BookingResponse> confirmBooking(
        @Parameter(description = "Booking ID") @PathVariable Long bookingId,
        @Parameter(description = "Payment ID") @RequestParam Long paymentId
    ) {
        String correlationId = CorrelationIdHolder.get();
        Long authenticatedUserId = SecurityUtil.getCurrentUserId();
        
        log.info("[{}] POST /bookings/{}/confirm - user: {}, paymentId: {}", 
                correlationId, bookingId, DataMaskingUtil.maskUserId(authenticatedUserId), paymentId);
        
        // Booking service will verify ownership
        Booking booking = bookingService.confirmBooking(bookingId, paymentId, authenticatedUserId);
        
        BookingResponse response = BookingResponse.builder()
            .bookingId(booking.getId())
            .bookingReference(booking.getBookingReference())
            .status(booking.getStatus().name())
            .finalAmount(booking.getFinalAmount())
            .qrCode("QR_" + booking.getBookingReference())
            .build();
        
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/{bookingId}/cancel")
    @Operation(summary = "Cancel booking", description = "Cancel an existing booking")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> cancelBooking(
        @Parameter(description = "Booking ID") @PathVariable Long bookingId,
        @Parameter(description = "Cancellation reason") @RequestParam(required = false) String reason
    ) {
        String correlationId = CorrelationIdHolder.get();
        Long authenticatedUserId = SecurityUtil.getCurrentUserId();
        
        log.info("[{}] POST /bookings/{}/cancel - user: {}, reason: {}", 
                correlationId, bookingId, DataMaskingUtil.maskUserId(authenticatedUserId), reason);
        
        // Booking service will verify ownership
        bookingService.cancelBooking(bookingId, authenticatedUserId, reason);
        return ResponseEntity.noContent().build();
    }
    
    @PostMapping("/payment/mock-success/{paymentId}")
    @Operation(summary = "Mock payment success", description = "Simulate successful payment (DEVELOPMENT ONLY)")
    @Profile({"dev", "test"})  // Only available in dev/test profiles
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PaymentResponse> mockPaymentSuccess(
        @Parameter(description = "Payment ID") @PathVariable Long paymentId
    ) {
        String correlationId = CorrelationIdHolder.get();
        Long authenticatedUserId = SecurityUtil.getCurrentUserId();
        
        log.warn("[{}] MOCK PAYMENT ENDPOINT CALLED - user: {}, paymentId: {}", 
                correlationId, DataMaskingUtil.maskUserId(authenticatedUserId), paymentId);
        
        PaymentResponse response = paymentService.mockPaymentSuccess(paymentId, authenticatedUserId);
        return ResponseEntity.ok(response);
    }
}
