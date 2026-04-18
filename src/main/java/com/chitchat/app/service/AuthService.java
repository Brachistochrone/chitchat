package com.chitchat.app.service;

import com.chitchat.app.dto.request.ChangePasswordRequest;
import com.chitchat.app.dto.request.LoginRequest;
import com.chitchat.app.dto.request.PasswordResetConfirmDto;
import com.chitchat.app.dto.request.PasswordResetRequestDto;
import com.chitchat.app.dto.request.RegisterRequest;
import com.chitchat.app.dto.response.AuthResponse;
import jakarta.servlet.http.HttpServletRequest;

public interface AuthService {

    AuthResponse register(RegisterRequest request, HttpServletRequest httpRequest);

    AuthResponse login(LoginRequest request, HttpServletRequest httpRequest);

    void logout(String sessionJti);

    void requestPasswordReset(PasswordResetRequestDto request);

    void confirmPasswordReset(PasswordResetConfirmDto request);

    void changePassword(Long userId, ChangePasswordRequest request);

    void deleteAccount(Long userId);
}
