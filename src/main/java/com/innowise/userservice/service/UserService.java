package com.innowise.userservice.service;

import com.innowise.userservice.config.CacheNames;
import com.innowise.userservice.dto.UserRequestDto;
import com.innowise.userservice.dto.UserResponseDto;
import com.innowise.userservice.entity.User;
import com.innowise.userservice.exception.EmailAlreadyExistsException;
import com.innowise.userservice.exception.UserNotFoundException;
import com.innowise.userservice.mapper.UserMapper;
import com.innowise.userservice.repository.UserRepository;
import com.innowise.userservice.specification.UserSpecifications;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Transactional
    public UserResponseDto create(UserRequestDto dto) {
        if (userRepository.existsByEmail(dto.email())) {
            throw new EmailAlreadyExistsException(dto.email());
        }
        User user = userMapper.toEntity(dto);
        return userMapper.toDto(userRepository.save(user));
    }

    @Cacheable(cacheNames = CacheNames.USERS, key = "#id")
    public UserResponseDto getById(Long id) {
        return userMapper.toDto(findUserWithCards(id));
    }

    public Page<UserResponseDto> getAll(String name, String surname, Pageable pageable) {
        return userRepository.findAll(UserSpecifications.byNameAndSurname(name, surname), pageable)
                .map(userMapper::toDto);
    }

    @CachePut(cacheNames = CacheNames.USERS, key = "#id")
    @Transactional
    public UserResponseDto update(Long id, UserRequestDto dto) {
        User user = findUserWithCards(id);
        if (userRepository.existsByEmailAndIdNot(dto.email(), id)) {
            throw new EmailAlreadyExistsException(dto.email());
        }
        userMapper.updateEntity(dto, user);
        return userMapper.toDto(userRepository.saveAndFlush(user));
    }

    @CacheEvict(cacheNames = CacheNames.USERS, key = "#id")
    @Transactional
    public void activate(Long id) {
        changeActiveStatus(id, true);
    }

    @CacheEvict(cacheNames = CacheNames.USERS, key = "#id")
    @Transactional
    public void deactivate(Long id) {
        changeActiveStatus(id, false);
    }

    @CacheEvict(cacheNames = CacheNames.USERS, key = "#id")
    @Transactional
    public void delete(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));
        userRepository.delete(user);
    }

    private void changeActiveStatus(Long id, boolean active) {
        if (userRepository.updateActiveStatus(id, active) == 0) {
            throw new UserNotFoundException(id);
        }
    }

    private User findUserWithCards(Long id) {
        return userRepository.findByIdWithCards(id)
                .orElseThrow(() -> new UserNotFoundException(id));
    }
}