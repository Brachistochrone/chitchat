package com.chitchat.app.service;

import com.chitchat.app.dao.UserRepository;
import com.chitchat.app.dao.UserSessionRepository;
import com.chitchat.app.dto.request.ChangePasswordRequest;
import com.chitchat.app.dto.request.LoginRequest;
import com.chitchat.app.dto.request.PasswordResetConfirmDto;
import com.chitchat.app.dto.request.PasswordResetRequestDto;
import com.chitchat.app.dto.request.RegisterRequest;
import com.chitchat.app.dto.response.AuthResponse;
import com.chitchat.app.entity.User;
import com.chitchat.app.entity.UserSession;
import com.chitchat.app.exception.ConflictException;
import com.chitchat.app.exception.ResourceNotFoundException;
import com.chitchat.app.exception.UnauthorizedException;
import com.chitchat.app.security.JwtTokenProvider;
import com.chitchat.app.util.AppConstants;
import com.chitchat.app.util.EntityMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final UserSessionRepository userSessionRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final JavaMailSender mailSender;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request, HttpServletRequest httpRequest) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException("Email is already taken");
        }
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new ConflictException("Username is already taken");
        }

        User user = User.builder()
                .email(request.getEmail())
                .username(request.getUsername())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .createdAt(OffsetDateTime.now())
                .build();
        user = userRepository.save(user);

        return buildAuthResponse(user, httpRequest);
    }

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request, HttpServletRequest httpRequest) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid email or password");
        }

        return buildAuthResponse(user, httpRequest);
    }

    @Override
    @Transactional
    public void logout(String sessionJti) {
        userSessionRepository.findByTokenHashAndRevokedFalse(sessionJti).ifPresent(session -> {
            session.setRevoked(true);
            userSessionRepository.save(session);
        });
    }

    @Override
    @Transactional
    public void requestPasswordReset(PasswordResetRequestDto request) {
        userRepository.findByEmail(request.getEmail()).ifPresent(user -> {
            String token = UUID.randomUUID().toString();
            user.setPasswordResetToken(token);
            user.setPasswordResetTokenExpiresAt(OffsetDateTime.now().plusHours(1));
            userRepository.save(user);

            try {
                SimpleMailMessage mail = new SimpleMailMessage();
                mail.setTo(user.getEmail());
                mail.setSubject("Chitchat — Password Reset");
                mail.setText("Your password reset token: " + token + "\nExpires in 1 hour.");
                mailSender.send(mail);
            } catch (Exception ex) {
                log.warn("Failed to send password reset email to {}: {}", user.getEmail(), ex.getMessage());
            }
        });
    }

    @Override
    @Transactional
    public void confirmPasswordReset(PasswordResetConfirmDto request) {
        User user = userRepository.findByPasswordResetToken(request.getToken())
                .orElseThrow(() -> new UnauthorizedException("Invalid or expired reset token"));

        if (user.getPasswordResetTokenExpiresAt() == null ||
                user.getPasswordResetTokenExpiresAt().isBefore(OffsetDateTime.now())) {
            throw new UnauthorizedException("Reset token has expired");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setPasswordResetToken(null);
        user.setPasswordResetTokenExpiresAt(null);
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new UnauthorizedException("Current password is incorrect");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void deleteAccount(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        user.setDeletedAt(OffsetDateTime.now());
        userRepository.save(user);
        log.info("Account soft-deleted for user id={}", userId);
    }

    private AuthResponse buildAuthResponse(User user, HttpServletRequest httpRequest) {
        String jti = jwtTokenProvider.generateJti();
        String token = jwtTokenProvider.generateToken(user.getId(), jti);

        UserSession session = UserSession.builder()
                .user(user)
                .tokenHash(jti)
                .browser(httpRequest.getHeader(AppConstants.USER_AGENT_HEADER))
                .ipAddress(httpRequest.getRemoteAddr())
                .createdAt(OffsetDateTime.now())
                .lastSeenAt(OffsetDateTime.now())
                .revoked(false)
                .build();
        userSessionRepository.save(session);

        return EntityMapper.toAuthResponse(token, user);
    }
}
