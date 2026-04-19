package com.chitchat.app.service;

import com.chitchat.app.dao.UserRepository;
import com.chitchat.app.dao.UserSessionRepository;
import com.chitchat.app.dto.request.ChangePasswordRequest;
import com.chitchat.app.dto.request.LoginRequest;
import com.chitchat.app.dto.request.PasswordResetConfirmDto;
import com.chitchat.app.dto.request.RegisterRequest;
import com.chitchat.app.dto.response.AuthResponse;
import com.chitchat.app.entity.User;
import com.chitchat.app.entity.UserSession;
import com.chitchat.app.exception.ConflictException;
import com.chitchat.app.exception.UnauthorizedException;
import com.chitchat.app.security.JwtTokenProvider;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private UserSessionRepository userSessionRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtTokenProvider jwtTokenProvider;
    @Mock private JavaMailSender mailSender;
    @Mock private HttpServletRequest httpRequest;

    @InjectMocks
    private AuthServiceImpl authService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .email("alice@example.com")
                .username("alice")
                .passwordHash("$hashed$")
                .createdAt(OffsetDateTime.now())
                .build();
    }

    @Test
    void register_success() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("alice@example.com");
        request.setPassword("password123");
        request.setUsername("alice");

        when(userRepository.existsByEmail("alice@example.com")).thenReturn(false);
        when(userRepository.existsByUsername("alice")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("$hashed$");
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(userSessionRepository.save(any(UserSession.class))).thenAnswer(i -> i.getArgument(0));
        when(jwtTokenProvider.generateJti()).thenReturn("jti-uuid");
        when(jwtTokenProvider.generateToken(1L, "jti-uuid")).thenReturn("jwt-token");
        when(httpRequest.getHeader("User-Agent")).thenReturn("TestBrowser");
        when(httpRequest.getRemoteAddr()).thenReturn("127.0.0.1");

        AuthResponse response = authService.register(request, httpRequest);

        assertThat(response.getToken()).isEqualTo("jwt-token");
        assertThat(response.getUser().getUsername()).isEqualTo("alice");
    }

    @Test
    void register_duplicateEmail_throwsConflict() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("alice@example.com");
        request.setPassword("password123");
        request.setUsername("alice");

        when(userRepository.existsByEmail("alice@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request, httpRequest))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Email");
    }

    @Test
    void register_duplicateUsername_throwsConflict() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("new@example.com");
        request.setPassword("password123");
        request.setUsername("alice");

        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(userRepository.existsByUsername("alice")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request, httpRequest))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Username");
    }

    @Test
    void login_success() {
        LoginRequest request = new LoginRequest();
        request.setEmail("alice@example.com");
        request.setPassword("password123");

        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("password123", "$hashed$")).thenReturn(true);
        when(userSessionRepository.save(any(UserSession.class))).thenAnswer(i -> i.getArgument(0));
        when(jwtTokenProvider.generateJti()).thenReturn("jti-uuid");
        when(jwtTokenProvider.generateToken(1L, "jti-uuid")).thenReturn("jwt-token");
        when(httpRequest.getHeader("User-Agent")).thenReturn("TestBrowser");
        when(httpRequest.getRemoteAddr()).thenReturn("127.0.0.1");

        AuthResponse response = authService.login(request, httpRequest);

        assertThat(response.getToken()).isEqualTo("jwt-token");
    }

    @Test
    void login_wrongPassword_throwsUnauthorized() {
        LoginRequest request = new LoginRequest();
        request.setEmail("alice@example.com");
        request.setPassword("wrong");

        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("wrong", "$hashed$")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(request, httpRequest))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void login_userNotFound_throwsUnauthorized() {
        LoginRequest request = new LoginRequest();
        request.setEmail("ghost@example.com");
        request.setPassword("password123");

        when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(request, httpRequest))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void logout_success() {
        UserSession session = UserSession.builder()
                .id(1L).tokenHash("jti-uuid").revoked(false).build();

        when(userSessionRepository.findByTokenHashAndRevokedFalse("jti-uuid"))
                .thenReturn(Optional.of(session));
        when(userSessionRepository.save(any(UserSession.class))).thenAnswer(i -> i.getArgument(0));

        authService.logout("jti-uuid");

        assertThat(session.isRevoked()).isTrue();
        verify(userSessionRepository).save(session);
    }

    @Test
    void changePassword_success() {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("oldPass");
        request.setNewPassword("newPass123");

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("oldPass", "$hashed$")).thenReturn(true);
        when(passwordEncoder.encode("newPass123")).thenReturn("$newHashed$");
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        authService.changePassword(1L, request);

        assertThat(testUser.getPasswordHash()).isEqualTo("$newHashed$");
    }

    @Test
    void changePassword_wrongCurrent_throwsUnauthorized() {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("wrongPass");
        request.setNewPassword("newPass123");

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("wrongPass", "$hashed$")).thenReturn(false);

        assertThatThrownBy(() -> authService.changePassword(1L, request))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void confirmPasswordReset_expiredToken_throwsUnauthorized() {
        PasswordResetConfirmDto request = new PasswordResetConfirmDto();
        request.setToken("reset-token");
        request.setNewPassword("newPass123");

        User userWithExpiredToken = User.builder()
                .id(1L)
                .passwordResetToken("reset-token")
                .passwordResetTokenExpiresAt(OffsetDateTime.now().minusHours(1))
                .build();

        when(userRepository.findByPasswordResetToken("reset-token"))
                .thenReturn(Optional.of(userWithExpiredToken));

        assertThatThrownBy(() -> authService.confirmPasswordReset(request))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("expired");
    }

    @Test
    void deleteAccount_success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        authService.deleteAccount(1L);

        assertThat(testUser.getDeletedAt()).isNotNull();
        verify(userRepository).save(testUser);
    }
}
