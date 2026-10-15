package com.innowise.userservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.innowise.userservice.dto.UserRequestDto;
import com.innowise.userservice.dto.UserResponseDto;
import com.innowise.userservice.entity.User;
import com.innowise.userservice.exception.EmailAlreadyExistsException;
import com.innowise.userservice.exception.UserNotFoundException;
import com.innowise.userservice.mapper.UserMapper;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    private static final Long USER_ID = 1L;
    private static final String EMAIL = "ivan@mail.com";

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserService userService;

    private UserRequestDto request;
    private User user;
    private UserResponseDto response;

    @BeforeEach
    void setUp() {
        request = new UserRequestDto("Ivan", "Petrov", LocalDate.of(1995, 5, 20), EMAIL);

        user = new User();
        user.setId(USER_ID);
        user.setName("Ivan");
        user.setSurname("Petrov");
        user.setBirthDate(LocalDate.of(1995, 5, 20));
        user.setEmail(EMAIL);

        response = new UserResponseDto(USER_ID, "Ivan", "Petrov", LocalDate.of(1995, 5, 20),
                EMAIL, true, null, null, List.of());
    }

    @Test
    void create_shouldSaveAndReturnUser_whenEmailIsFree() {
        when(userRepository.existsByEmail(EMAIL)).thenReturn(false);
        when(userMapper.toEntity(request)).thenReturn(user);
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toDto(user)).thenReturn(response);

        UserResponseDto result = userService.create(request);

        assertThat(result).isEqualTo(response);
        verify(userRepository).save(user);
    }

    @Test
    void create_shouldThrow_whenEmailAlreadyExists() {
        when(userRepository.existsByEmail(EMAIL)).thenReturn(true);

        assertThatThrownBy(() -> userService.create(request))
                .isInstanceOf(EmailAlreadyExistsException.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    void getById_shouldReturnUser_whenUserExists() {
        when(userRepository.findByIdWithCards(USER_ID)).thenReturn(Optional.of(user));
        when(userMapper.toDto(user)).thenReturn(response);

        UserResponseDto result = userService.getById(USER_ID);

        assertThat(result).isEqualTo(response);
    }

    @Test
    void getById_shouldThrow_whenUserNotFound() {
        when(userRepository.findByIdWithCards(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getById(USER_ID))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void getAll_shouldReturnPageOfUsers() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<User> page = new PageImpl<>(List.of(user), pageable, 1);
        when(userRepository.findAll(ArgumentMatchers.<Specification<User>>any(), eq(pageable)))
                .thenReturn(page);
        when(userMapper.toDto(user)).thenReturn(response);

        Page<UserResponseDto> result = userService.getAll("Ivan", null, pageable);

        assertThat(result.getContent()).containsExactly(response);
        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    void update_shouldUpdateAndReturnUser_whenDataIsValid() {
        when(userRepository.findByIdWithCards(USER_ID)).thenReturn(Optional.of(user));
        when(userRepository.existsByEmailAndIdNot(EMAIL, USER_ID)).thenReturn(false);
        when(userRepository.saveAndFlush(user)).thenReturn(user);
        when(userMapper.toDto(user)).thenReturn(response);

        UserResponseDto result = userService.update(USER_ID, request);

        assertThat(result).isEqualTo(response);
        verify(userMapper).updateEntity(request, user);
    }

    @Test
    void update_shouldThrow_whenUserNotFound() {
        when(userRepository.findByIdWithCards(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.update(USER_ID, request))
                .isInstanceOf(UserNotFoundException.class);
        verify(userMapper, never()).updateEntity(any(), any());
    }

    @Test
    void update_shouldThrow_whenEmailBelongsToAnotherUser() {
        when(userRepository.findByIdWithCards(USER_ID)).thenReturn(Optional.of(user));
        when(userRepository.existsByEmailAndIdNot(EMAIL, USER_ID)).thenReturn(true);

        assertThatThrownBy(() -> userService.update(USER_ID, request))
                .isInstanceOf(EmailAlreadyExistsException.class);
        verify(userRepository, never()).saveAndFlush(any());
    }

    @Test
    void activate_shouldUpdateStatus_whenUserExists() {
        when(userRepository.updateActiveStatus(USER_ID, true)).thenReturn(1);

        userService.activate(USER_ID);

        verify(userRepository).updateActiveStatus(USER_ID, true);
    }

    @Test
    void activate_shouldThrow_whenUserNotFound() {
        when(userRepository.updateActiveStatus(USER_ID, true)).thenReturn(0);

        assertThatThrownBy(() -> userService.activate(USER_ID))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void deactivate_shouldUpdateStatus_whenUserExists() {
        when(userRepository.updateActiveStatus(USER_ID, false)).thenReturn(1);

        userService.deactivate(USER_ID);

        verify(userRepository).updateActiveStatus(USER_ID, false);
    }

    @Test
    void deactivate_shouldThrow_whenUserNotFound() {
        when(userRepository.updateActiveStatus(USER_ID, false)).thenReturn(0);

        assertThatThrownBy(() -> userService.deactivate(USER_ID))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void delete_shouldDeleteUser_whenUserExists() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

        userService.delete(USER_ID);

        verify(userRepository).delete(user);
    }

    @Test
    void delete_shouldThrow_whenUserNotFound() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.delete(USER_ID))
                .isInstanceOf(UserNotFoundException.class);
        verify(userRepository, never()).delete(any(User.class));
    }
}