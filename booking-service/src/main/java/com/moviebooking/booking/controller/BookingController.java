package com.moviebooking.booking.controller;

import com.moviebooking.booking.dto.*;
import com.moviebooking.booking.model.Booking;
import com.moviebooking.booking.service.BookingService;
import com.moviebooking.booking.service.PaymentService;
import com.moviebooking.common.dto.SeatDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.stream.Collectors;

@RestController
@RequestMapping("/bookings")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Booking", description = "APIs for movie ticket booking with seat locking")
public class BookingController {
    
    private final BookingService bookingService;
    private final PaymentService paymentService;
    
    @PostMapping("/hold")
    @Operation(summary = "Hold seats", description = "Temporarily hold seats for 10 minutes before booking")
    public ResponseEntity<HoldSeatsResponse> holdSeats(@Valid @RequestBody HoldSeatsRequest request) {
        log.info("POST /bookings/hold - session: {}, seats: {}", request.getSessionId(), request.getSeatIds());
        HoldSeatsResponse response = bookingService.holdSeats(request);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping
    @Operation(summary = "Create booking", description = "Create a booking for held seats")
    public ResponseEntity<BookingResponse> createBooking(@Valid @RequestBody BookingRequest request) {
        log.info("POST /bookings - user: {}, show: {}, seats: {}", 
            request.getUserId(), request.getShowId(), request.getSeatIds());
        
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
    public ResponseEntity<BookingResponse> confirmBooking(
        @Parameter(description = "Booking ID") @PathVariable Long bookingId,
        @Parameter(description = "Payment ID") @RequestParam Long paymentId
    ) {
        log.info("POST /bookings/{}/confirm - paymentId: {}", bookingId, paymentId);
        
        Booking booking = bookingService.confirmBooking(bookingId, paymentId);
        
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
    public ResponseEntity<Void> cancelBooking(
        @Parameter(description = "Booking ID") @PathVariable Long bookingId,
        @Parameter(description = "Cancellation reason") @RequestParam(required = false) String reason
    ) {
        log.info("POST /bookings/{}/cancel - reason: {}", bookingId, reason);
        bookingService.cancelBooking(bookingId, reason);
        return ResponseEntity.noContent().build();
    }
    
    @PostMapping("/payment/mock-success/{paymentId}")
    @Operation(summary = "Mock payment success", description = "Simulate successful payment (for testing)")
    public ResponseEntity<PaymentResponse> mockPaymentSuccess(
        @Parameter(description = "Payment ID") @PathVariable Long paymentId
    ) {
        log.info("POST /bookings/payment/mock-success/{}", paymentId);
        PaymentResponse response = paymentService.mockPaymentSuccess(paymentId);
        return ResponseEntity.ok(response);
    }
}
