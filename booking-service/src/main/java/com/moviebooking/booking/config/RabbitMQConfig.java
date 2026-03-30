package com.moviebooking.booking.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {
    
    public static final String EXCHANGE = "booking.exchange";
    public static final String QUEUE_BOOKING_CONFIRMED = "booking.confirmed.queue";
    public static final String QUEUE_BOOKING_CANCELLED = "booking.cancelled.queue";
    public static final String QUEUE_PAYMENT_SUCCESS = "payment.success.queue";
    
    @Bean
    public TopicExchange bookingExchange() {
        return new TopicExchange(EXCHANGE);
    }
    
    @Bean
    public Queue bookingConfirmedQueue() {
        return new Queue(QUEUE_BOOKING_CONFIRMED, true);
    }
    
    @Bean
    public Queue bookingCancelledQueue() {
        return new Queue(QUEUE_BOOKING_CANCELLED, true);
    }
    
    @Bean
    public Queue paymentSuccessQueue() {
        return new Queue(QUEUE_PAYMENT_SUCCESS, true);
    }
    
    @Bean
    public Binding bookingConfirmedBinding() {
        return BindingBuilder
            .bind(bookingConfirmedQueue())
            .to(bookingExchange())
            .with("booking.confirmed");
    }
    
    @Bean
    public Binding bookingCancelledBinding() {
        return BindingBuilder
            .bind(bookingCancelledQueue())
            .to(bookingExchange())
            .with("booking.cancelled");
    }
    
    @Bean
    public Binding paymentSuccessBinding() {
        return BindingBuilder
            .bind(paymentSuccessQueue())
            .to(bookingExchange())
            .with("payment.success");
    }
    
    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }
    
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter());
        return template;
    }
}
