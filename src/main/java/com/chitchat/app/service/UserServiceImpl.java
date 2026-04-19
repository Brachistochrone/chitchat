package com.chitchat.app.service;

import com.chitchat.app.dao.UserRepository;
import com.chitchat.app.dto.request.UpdateProfileRequest;
import com.chitchat.app.dto.response.UserResponse;
import com.chitchat.app.entity.User;
import com.chitchat.app.exception.ResourceNotFoundException;
import com.chitchat.app.util.EntityMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Override
    public UserResponse getMe(Long userId) {
        return EntityMapper.toUserResponse(findActiveUser(userId));
    }

    @Override
    @Transactional
    @CacheEvict(value = "users", key = "#userId")
    public UserResponse updateProfile(Long userId, UpdateProfileRequest request) {
        User user = findActiveUser(userId);
        user.setDisplayName(request.getDisplayName());
        userRepository.save(user);
        return EntityMapper.toUserResponse(user);
    }

    @Override
    public UserResponse getUserByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
        return EntityMapper.toUserResponse(user);
    }

    private User findActiveUser(Long userId) {
        return userRepository.findById(userId)
                .filter(u -> u.getDeletedAt() == null)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
