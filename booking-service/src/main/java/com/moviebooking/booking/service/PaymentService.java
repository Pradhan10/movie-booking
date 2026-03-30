package com.moviebooking.booking.service;

import com.moviebooking.booking.dto.PaymentRequest;
import com.moviebooking.booking.dto.PaymentResponse;
import com.moviebooking.booking.event.PaymentSuccessEvent;
import com.moviebooking.booking.model.Payment;
import com.moviebooking.booking.repository.PaymentRepository;
import com.moviebooking.common.enums.PaymentStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {
    
    private final PaymentRepository paymentRepository;
    private final BookingService bookingService;
    private final EventPublisher eventPublisher;
    
    @Value("${booking.payment.gateway-url}")
    private String gatewayUrl;
    
    @Transactional
    public PaymentResponse initiatePayment(PaymentRequest request) {
        log.info("Initiating payment for booking: {}, amount: {}", 
            request.getBookingId(), request.getAmount());
        
        Payment payment = Payment.builder()
            .bookingId(request.getBookingId())
            .amount(request.getAmount())
            .paymentMethod(request.getPaymentMethod())
            .gatewayName("RAZORPAY")
            .status(PaymentStatus.INITIATED)
            .build();
        
        payment = paymentRepository.save(payment);
        
        String paymentUrl = gatewayUrl + "/checkout/" + payment.getId();
        
        return PaymentResponse.builder()
            .paymentId(payment.getId())
            .bookingId(request.getBookingId())
            .amount(request.getAmount())
            .paymentUrl(paymentUrl)
            .status("INITIATED")
            .build();
    }
    
    @Transactional
    public void handleWebhook(String gatewayTxnId, String status, String response) {
        log.info("Handling payment webhook - txnId: {}, status: {}", gatewayTxnId, status);
        
        Payment payment = paymentRepository.findByGatewayTxnId(gatewayTxnId)
            .orElseThrow(() -> new RuntimeException("Payment not found for txn: " + gatewayTxnId));
        
        if ("SUCCESS".equals(status)) {
            payment.markSuccess(gatewayTxnId, response);
            paymentRepository.save(payment);
            
            bookingService.confirmBooking(payment.getBookingId(), payment.getId());
            
            PaymentSuccessEvent event = PaymentSuccessEvent.builder()
                .paymentId(payment.getId())
                .bookingId(payment.getBookingId())
                .amount(payment.getAmount())
                .gatewayTxnId(gatewayTxnId)
                .completedAt(Instant.now())
                .build();
            
            eventPublisher.publishPaymentSuccess(event);
            
        } else {
            payment.markFailed(response);
            paymentRepository.save(payment);
            
            bookingService.cancelBooking(payment.getBookingId(), "Payment failed");
        }
    }
    
    @Transactional
    public PaymentResponse mockPaymentSuccess(Long paymentId) {
        log.info("Mock payment success for payment: {}", paymentId);
        
        Payment payment = paymentRepository.findById(paymentId)
            .orElseThrow(() -> new RuntimeException("Payment not found: " + paymentId));
        
        String mockTxnId = "TXN_" + UUID.randomUUID().toString();
        payment.markSuccess(mockTxnId, "{\"status\": \"success\"}");
        payment = paymentRepository.save(payment);
        
        bookingService.confirmBooking(payment.getBookingId(), payment.getId());
        
        PaymentSuccessEvent event = PaymentSuccessEvent.builder()
            .paymentId(payment.getId())
            .bookingId(payment.getBookingId())
            .amount(payment.getAmount())
            .gatewayTxnId(mockTxnId)
            .completedAt(Instant.now())
            .build();
        
        eventPublisher.publishPaymentSuccess(event);
        
        return PaymentResponse.builder()
            .paymentId(payment.getId())
            .bookingId(payment.getBookingId())
            .amount(payment.getAmount())
            .status("SUCCESS")
            .gatewayTxnId(mockTxnId)
            .build();
    }
}
