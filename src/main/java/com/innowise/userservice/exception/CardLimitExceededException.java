package com.innowise.userservice.exception;

public class CardLimitExceededException extends RuntimeException {
    public CardLimitExceededException(Long userId, int limit) {
        super("User with id " + userId + " already has the maximum of " + limit + " cards");
    }
}