package com.moviebooking.booking.exception;

public class HoldExpiredException extends RuntimeException {
    public HoldExpiredException(String message) {
        super(message);
    }
}
