package com.innowise.userservice.exception;

public class CardNumberAlreadyExistsException extends RuntimeException {
    public CardNumberAlreadyExistsException() {
        super("Payment card with this number already exists");
    }
}