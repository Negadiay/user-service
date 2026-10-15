package com.innowise.userservice.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record UserResponseDto(
        Long id,
        String name,
        String surname,
        LocalDate birthDate,
        String email,
        Boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<PaymentCardResponseDto> cards
) {
}