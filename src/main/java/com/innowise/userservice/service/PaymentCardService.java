package com.innowise.userservice.service;

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
import com.innowise.userservice.specification.PaymentCardSpecifications;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentCardService {

    private final PaymentCardRepository cardRepository;
    private final UserRepository userRepository;
    private final PaymentCardMapper cardMapper;
    private final CacheManager cacheManager;

    @Transactional
    public PaymentCardResponseDto create(Long userId, PaymentCardRequestDto dto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        if (cardRepository.countCardsByUserId(userId) >= User.MAX_CARDS) {
            throw new CardLimitExceededException(userId, User.MAX_CARDS);
        }
        if (cardRepository.existsByNumber(dto.number())) {
            throw new CardNumberAlreadyExistsException();
        }
        PaymentCard card = cardMapper.toEntity(dto);
        user.addCard(card);
        PaymentCardResponseDto result = cardMapper.toDto(cardRepository.save(card));
        evictUserCache(userId);
        return result;
    }

    public PaymentCardResponseDto getById(Long id) {
        return cardMapper.toDto(findCard(id));
    }

    public Page<PaymentCardResponseDto> getAll(String name, String surname, Pageable pageable) {
        return cardRepository.findAll(PaymentCardSpecifications.byUserNameAndSurname(name, surname), pageable)
                .map(cardMapper::toDto);
    }

    public List<PaymentCardResponseDto> getAllByUserId(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new UserNotFoundException(userId);
        }
        return cardMapper.toDtoList(cardRepository.findAllByUserId(userId));
    }

    @Transactional
    public PaymentCardResponseDto update(Long id, PaymentCardRequestDto dto) {
        PaymentCard card = findCard(id);
        if (cardRepository.existsByNumberAndIdNot(dto.number(), id)) {
            throw new CardNumberAlreadyExistsException();
        }
        cardMapper.updateEntity(dto, card);
        PaymentCardResponseDto result = cardMapper.toDto(cardRepository.saveAndFlush(card));
        evictUserCache(result.userId());
        return result;
    }

    @Transactional
    public void activate(Long id) {
        changeActiveStatus(id, true);
    }

    @Transactional
    public void deactivate(Long id) {
        changeActiveStatus(id, false);
    }

    @Transactional
    public void delete(Long id) {
        PaymentCard card = findCard(id);
        Long userId = card.getUser().getId();
        cardRepository.delete(card);
        evictUserCache(userId);
    }

    private void changeActiveStatus(Long id, boolean active) {
        Long userId = cardRepository.findUserIdByCardId(id)
                .orElseThrow(() -> new PaymentCardNotFoundException(id));
        cardRepository.updateActiveStatus(id, active);
        evictUserCache(userId);
    }

    private void evictUserCache(Long userId) {
        Cache cache = cacheManager.getCache(CacheNames.USERS);
        if (cache != null) {
            cache.evict(userId);
        }
    }

    private PaymentCard findCard(Long id) {
        return cardRepository.findById(id)
                .orElseThrow(() -> new PaymentCardNotFoundException(id));
    }
}