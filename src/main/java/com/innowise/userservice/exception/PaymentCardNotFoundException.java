package com.innowise.userservice.exception;

public class PaymentCardNotFoundException extends RuntimeException {
    public PaymentCardNotFoundException(Long id) {
        super("Payment card with id " + id + " not found");
    }
}