package com.innowise.userservice.controller;

import com.innowise.userservice.dto.PaymentCardRequestDto;
import com.innowise.userservice.dto.PaymentCardResponseDto;
import com.innowise.userservice.service.PaymentCardService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class PaymentCardController {
    private final PaymentCardService cardService;

    @PostMapping("/users/{userId}/cards")
    public ResponseEntity<PaymentCardResponseDto> create(@PathVariable Long userId,
                                                         @Valid @RequestBody PaymentCardRequestDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(cardService.create(userId, dto));
    }

    @GetMapping("/users/{userId}/cards")
    public ResponseEntity<List<PaymentCardResponseDto>> getAllByUserId(@PathVariable Long userId) {
        return ResponseEntity.ok(cardService.getAllByUserId(userId));
    }

    @GetMapping("/cards/{id}")
    public ResponseEntity<PaymentCardResponseDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(cardService.getById(id));
    }

    @GetMapping("/cards")
    public ResponseEntity<Page<PaymentCardResponseDto>> getAll(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String surname,
            @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return ResponseEntity.ok(cardService.getAll(name, surname, pageable));
    }

    @PutMapping("/cards/{id}")
    public ResponseEntity<PaymentCardResponseDto> update(@PathVariable Long id,
                                                         @Valid @RequestBody PaymentCardRequestDto dto) {
        return ResponseEntity.ok(cardService.update(id, dto));
    }

    @PatchMapping("/cards/{id}/activate")
    public ResponseEntity<Void> activate(@PathVariable Long id) {
        cardService.activate(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/cards/{id}/deactivate")
    public ResponseEntity<Void> deactivate(@PathVariable Long id) {
        cardService.deactivate(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/cards/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        cardService.delete(id);
        return ResponseEntity.noContent().build();
    }
}