package com.innowise.userservice.dto;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record PaymentCardResponseDto(
        Long id,
        Long userId,
        String number,
        String holder,
        LocalDate expirationDate,
        Boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) implements Serializable {
}