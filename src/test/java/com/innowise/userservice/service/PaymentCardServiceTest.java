package com.innowise.userservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.innowise.userservice.config.CacheNames;
import com.innowise.userservice.dto.PaymentCardRequestDto;
import com.innowise.userservice.dto.PaymentCardResponseDto;
import com.innowise.userservice.entity.PaymentCard;
import com.innowise.userservice.entity.User;
import com.innowise.userservice.exception.CardLimitExceededException;
import com.innowise.userservice.exception.CardNumberAlreadyExistsException;
import com.innowise.userservice.exception.PaymentCardNotFoundException;
import com.innowise.userservice.exception.UserNotFoundException;
import com.innowise.userservice.mapper.PaymentCardMapper;
import com.innowise.userservice.repository.PaymentCardRepository;
import com.innowise.userservice.repository.UserRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class PaymentCardServiceTest {
    private static final Long USER_ID = 1L;
    private static final Long CARD_ID = 10L;
    private static final String NUMBER = "4111111111111111";
    private static final LocalDate EXPIRATION = LocalDate.of(2029, 12, 31);

    @Mock
    private PaymentCardRepository cardRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PaymentCardMapper cardMapper;

    @Mock
    private CacheManager cacheManager;

    @Mock
    private Cache cache;

    @InjectMocks
    private PaymentCardService cardService;

    private User user;
    private PaymentCard card;
    private PaymentCardRequestDto request;
    private PaymentCardResponseDto response;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(USER_ID);

        card = new PaymentCard();
        card.setId(CARD_ID);
        card.setNumber(NUMBER);
        card.setHolder("IVAN PETROV");
        card.setExpirationDate(EXPIRATION);
        card.setUser(user);

        request = new PaymentCardRequestDto(NUMBER, "IVAN PETROV", EXPIRATION);
        response = new PaymentCardResponseDto(CARD_ID, USER_ID, NUMBER, "IVAN PETROV",
                EXPIRATION, true, null, null);
    }

    @Test
    void create_shouldSaveCardAndEvictUserCache_whenDataIsValid() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(cardRepository.countCardsByUserId(USER_ID)).thenReturn(0L);
        when(cardRepository.existsByNumber(NUMBER)).thenReturn(false);
        when(cardMapper.toEntity(request)).thenReturn(card);
        when(cardRepository.save(card)).thenReturn(card);
        when(cardMapper.toDto(card)).thenReturn(response);
        when(cacheManager.getCache(CacheNames.USERS)).thenReturn(cache);

        PaymentCardResponseDto result = cardService.create(USER_ID, request);

        assertThat(result).isEqualTo(response);
        assertThat(user.getCards()).containsExactly(card);
        assertThat(card.getUser()).isSameAs(user);
        verify(cache).evict(USER_ID);
    }

    @Test
    void create_shouldThrow_whenUserNotFound() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cardService.create(USER_ID, request))
                .isInstanceOf(UserNotFoundException.class);
        verify(cardRepository, never()).save(any(PaymentCard.class));
    }

    @Test
    void create_shouldThrow_whenUserHasMaxCards() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(cardRepository.countCardsByUserId(USER_ID)).thenReturn((long) User.MAX_CARDS);

        assertThatThrownBy(() -> cardService.create(USER_ID, request))
                .isInstanceOf(CardLimitExceededException.class);
        verify(cardRepository, never()).save(any(PaymentCard.class));
    }

    @Test
    void create_shouldThrow_whenCardNumberAlreadyExists() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(cardRepository.countCardsByUserId(USER_ID)).thenReturn(0L);
        when(cardRepository.existsByNumber(NUMBER)).thenReturn(true);

        assertThatThrownBy(() -> cardService.create(USER_ID, request))
                .isInstanceOf(CardNumberAlreadyExistsException.class);
        verify(cardRepository, never()).save(any(PaymentCard.class));
    }

    @Test
    void getById_shouldReturnCard_whenCardExists() {
        when(cardRepository.findById(CARD_ID)).thenReturn(Optional.of(card));
        when(cardMapper.toDto(card)).thenReturn(response);

        assertThat(cardService.getById(CARD_ID)).isEqualTo(response);
    }

    @Test
    void getById_shouldThrow_whenCardNotFound() {
        when(cardRepository.findById(CARD_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cardService.getById(CARD_ID))
                .isInstanceOf(PaymentCardNotFoundException.class);
    }

    @Test
    void getAll_shouldReturnPageOfCards() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<PaymentCard> page = new PageImpl<>(List.of(card), pageable, 1);
        when(cardRepository.findAll(ArgumentMatchers.<Specification<PaymentCard>>any(), eq(pageable)))
                .thenReturn(page);
        when(cardMapper.toDto(card)).thenReturn(response);

        Page<PaymentCardResponseDto> result = cardService.getAll("Ivan", "Petrov", pageable);

        assertThat(result.getContent()).containsExactly(response);
    }

    @Test
    void getAllByUserId_shouldReturnCards_whenUserExists() {
        when(userRepository.existsById(USER_ID)).thenReturn(true);
        when(cardRepository.findAllByUserId(USER_ID)).thenReturn(List.of(card));
        when(cardMapper.toDtoList(List.of(card))).thenReturn(List.of(response));

        assertThat(cardService.getAllByUserId(USER_ID)).containsExactly(response);
    }

    @Test
    void getAllByUserId_shouldThrow_whenUserNotFound() {
        when(userRepository.existsById(USER_ID)).thenReturn(false);

        assertThatThrownBy(() -> cardService.getAllByUserId(USER_ID))
                .isInstanceOf(UserNotFoundException.class);
        verify(cardRepository, never()).findAllByUserId(anyLong());
    }

    @Test
    void update_shouldUpdateCardAndEvictUserCache_whenDataIsValid() {
        when(cardRepository.findById(CARD_ID)).thenReturn(Optional.of(card));
        when(cardRepository.existsByNumberAndIdNot(NUMBER, CARD_ID)).thenReturn(false);
        when(cardRepository.saveAndFlush(card)).thenReturn(card);
        when(cardMapper.toDto(card)).thenReturn(response);
        when(cacheManager.getCache(CacheNames.USERS)).thenReturn(cache);

        PaymentCardResponseDto result = cardService.update(CARD_ID, request);

        assertThat(result).isEqualTo(response);
        verify(cardMapper).updateEntity(request, card);
        verify(cache).evict(USER_ID);
    }

    @Test
    void update_shouldThrow_whenCardNotFound() {
        when(cardRepository.findById(CARD_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cardService.update(CARD_ID, request))
                .isInstanceOf(PaymentCardNotFoundException.class);
    }

    @Test
    void update_shouldThrow_whenNumberBelongsToAnotherCard() {
        when(cardRepository.findById(CARD_ID)).thenReturn(Optional.of(card));
        when(cardRepository.existsByNumberAndIdNot(NUMBER, CARD_ID)).thenReturn(true);

        assertThatThrownBy(() -> cardService.update(CARD_ID, request))
                .isInstanceOf(CardNumberAlreadyExistsException.class);
        verify(cardRepository, never()).saveAndFlush(any(PaymentCard.class));
    }

    @Test
    void activate_shouldUpdateStatusAndEvictUserCache_whenCardExists() {
        when(cardRepository.findUserIdByCardId(CARD_ID)).thenReturn(Optional.of(USER_ID));
        when(cacheManager.getCache(CacheNames.USERS)).thenReturn(cache);

        cardService.activate(CARD_ID);

        verify(cardRepository).updateActiveStatus(CARD_ID, true);
        verify(cache).evict(USER_ID);
    }

    @Test
    void activate_shouldThrow_whenCardNotFound() {
        when(cardRepository.findUserIdByCardId(CARD_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cardService.activate(CARD_ID))
                .isInstanceOf(PaymentCardNotFoundException.class);
        verify(cardRepository, never()).updateActiveStatus(anyLong(), anyBoolean());
    }

    @Test
    void deactivate_shouldUpdateStatusAndEvictUserCache_whenCardExists() {
        when(cardRepository.findUserIdByCardId(CARD_ID)).thenReturn(Optional.of(USER_ID));
        when(cacheManager.getCache(CacheNames.USERS)).thenReturn(cache);

        cardService.deactivate(CARD_ID);

        verify(cardRepository).updateActiveStatus(CARD_ID, false);
        verify(cache).evict(USER_ID);
    }

    @Test
    void delete_shouldDeleteCardAndEvictUserCache_whenCardExists() {
        when(cardRepository.findById(CARD_ID)).thenReturn(Optional.of(card));
        when(cacheManager.getCache(CacheNames.USERS)).thenReturn(cache);

        cardService.delete(CARD_ID);

        verify(cardRepository).delete(card);
        verify(cache).evict(USER_ID);
    }

    @Test
    void delete_shouldThrow_whenCardNotFound() {
        when(cardRepository.findById(CARD_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cardService.delete(CARD_ID))
                .isInstanceOf(PaymentCardNotFoundException.class);
        verify(cardRepository, never()).delete(any(PaymentCard.class));
    }
}