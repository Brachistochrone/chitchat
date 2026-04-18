package com.chitchat.app.service;

import com.chitchat.app.dao.UserRepository;
import com.chitchat.app.dto.request.UpdateProfileRequest;
import com.chitchat.app.dto.response.UserResponse;
import com.chitchat.app.entity.User;
import com.chitchat.app.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock private UserRepository userRepository;

    @InjectMocks
    private UserServiceImpl userService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .email("alice@example.com")
                .username("alice")
                .displayName("Alice")
                .passwordHash("$hashed$")
                .createdAt(OffsetDateTime.now())
                .build();
    }

    @Test
    void getMe_success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        UserResponse response = userService.getMe(1L);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getUsername()).isEqualTo("alice");
    }

    @Test
    void getMe_notFound_throwsResourceNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getMe(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getMe_deletedUser_throwsResourceNotFound() {
        testUser.setDeletedAt(OffsetDateTime.now());
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        assertThatThrownBy(() -> userService.getMe(1L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateProfile_success() {
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setDisplayName("Alice Updated");

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        UserResponse response = userService.updateProfile(1L, request);

        assertThat(response.getDisplayName()).isEqualTo("Alice Updated");
    }

    @Test
    void getUserByUsername_success() {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(testUser));

        UserResponse response = userService.getUserByUsername("alice");

        assertThat(response.getUsername()).isEqualTo("alice");
    }

    @Test
    void getUserByUsername_notFound_throwsResourceNotFound() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserByUsername("ghost"))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
