package com.innowise.userservice.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record PaymentCardRequestDto(
        @NotBlank
        @Pattern(regexp = "\\d{16}", message = "Номер карты должен состоять ровно из 16 цифр")
        String number,
        @NotBlank @Size(max = 255) String holder,
        @NotNull @FutureOrPresent LocalDate expirationDate
) {
}