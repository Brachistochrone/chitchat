package com.chitchat.app.rest;

import com.chitchat.app.dto.request.LoginRequest;
import com.chitchat.app.dto.request.PasswordResetConfirmDto;
import com.chitchat.app.dto.request.PasswordResetRequestDto;
import com.chitchat.app.dto.request.RegisterRequest;
import com.chitchat.app.dto.response.AuthResponse;
import com.chitchat.app.security.JwtTokenProvider;
import com.chitchat.app.service.AuthService;
import com.chitchat.app.util.HttpUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Auth", description = "Authentication endpoints")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final JwtTokenProvider jwtTokenProvider;

    @Operation(summary = "Register a new user")
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request,
                                                 HttpServletRequest httpRequest) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request, httpRequest));
    }

    @Operation(summary = "Login")
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request,
                                              HttpServletRequest httpRequest) {
        return ResponseEntity.ok(authService.login(request, httpRequest));
    }

    @Operation(summary = "Logout current session")
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest httpRequest) {
        String token = HttpUtil.extractBearerToken(httpRequest);
        if (StringUtils.hasText(token) && jwtTokenProvider.validateToken(token)) {
            authService.logout(jwtTokenProvider.getJtiFromToken(token));
        }
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Request password reset email")
    @PostMapping("/password-reset/request")
    public ResponseEntity<Void> requestPasswordReset(@Valid @RequestBody PasswordResetRequestDto request) {
        authService.requestPasswordReset(request);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Confirm password reset with token")
    @PostMapping("/password-reset/confirm")
    public ResponseEntity<Void> confirmPasswordReset(@Valid @RequestBody PasswordResetConfirmDto request) {
        authService.confirmPasswordReset(request);
        return ResponseEntity.noContent().build();
    }
}
