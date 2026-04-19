package com.chitchat.app.service;

import com.chitchat.app.dto.request.UpdateProfileRequest;
import com.chitchat.app.dto.response.UserResponse;

public interface UserService {

    UserResponse getMe(Long userId);

    UserResponse updateProfile(Long userId, UpdateProfileRequest request);

    UserResponse getUserByUsername(String username);
}
