package com.moviebooking.booking.service;

import com.moviebooking.booking.event.BookingConfirmedEvent;
import com.moviebooking.booking.event.BookingCancelledEvent;
import com.moviebooking.booking.event.PaymentSuccessEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventPublisher {
    
    private final RabbitTemplate rabbitTemplate;
    
    private static final String EXCHANGE = "booking.exchange";
    
    public void publishBookingConfirmed(BookingConfirmedEvent event) {
        try {
            rabbitTemplate.convertAndSend(EXCHANGE, "booking.confirmed", event);
            log.info("Published BookingConfirmedEvent for booking: {}", event.getBookingId());
        } catch (Exception e) {
            log.error("Failed to publish BookingConfirmedEvent for booking: {}", event.getBookingId(), e);
        }
    }
    
    public void publishBookingCancelled(BookingCancelledEvent event) {
        try {
            rabbitTemplate.convertAndSend(EXCHANGE, "booking.cancelled", event);
            log.info("Published BookingCancelledEvent for booking: {}", event.getBookingId());
        } catch (Exception e) {
            log.error("Failed to publish BookingCancelledEvent for booking: {}", event.getBookingId(), e);
        }
    }
    
    public void publishPaymentSuccess(PaymentSuccessEvent event) {
        try {
            rabbitTemplate.convertAndSend(EXCHANGE, "payment.success", event);
            log.info("Published PaymentSuccessEvent for payment: {}", event.getPaymentId());
        } catch (Exception e) {
            log.error("Failed to publish PaymentSuccessEvent for payment: {}", event.getPaymentId(), e);
        }
    }
}
