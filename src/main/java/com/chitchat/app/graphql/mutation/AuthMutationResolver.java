package com.chitchat.app.graphql.mutation;

import com.chitchat.app.dto.request.ChangePasswordRequest;
import com.chitchat.app.dto.request.LoginRequest;
import com.chitchat.app.dto.request.PasswordResetConfirmDto;
import com.chitchat.app.dto.request.PasswordResetRequestDto;
import com.chitchat.app.dto.request.RegisterRequest;
import com.chitchat.app.dto.response.AuthResponse;
import com.chitchat.app.security.JwtTokenProvider;
import com.chitchat.app.service.AuthService;
import com.chitchat.app.util.HttpUtil;
import com.chitchat.app.util.SecurityUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.stereotype.Controller;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Controller
@RequiredArgsConstructor
public class AuthMutationResolver {

    private final AuthService authService;
    private final JwtTokenProvider jwtTokenProvider;

    @MutationMapping
    public AuthResponse register(@Argument String email, @Argument String password,
                                  @Argument String username) {
        RegisterRequest request = new RegisterRequest();
        request.setEmail(email);
        request.setPassword(password);
        request.setUsername(username);
        return authService.register(request, httpRequest());
    }

    @MutationMapping
    public AuthResponse login(@Argument String email, @Argument String password) {
        LoginRequest request = new LoginRequest();
        request.setEmail(email);
        request.setPassword(password);
        return authService.login(request, httpRequest());
    }

    @MutationMapping
    public boolean logout() {
        String token = HttpUtil.extractBearerToken(httpRequest());
        if (token != null) {
            authService.logout(jwtTokenProvider.getJtiFromToken(token));
        }
        return true;
    }

    @MutationMapping
    public boolean requestPasswordReset(@Argument String email) {
        PasswordResetRequestDto dto = new PasswordResetRequestDto();
        dto.setEmail(email);
        authService.requestPasswordReset(dto);
        return true;
    }

    @MutationMapping
    public boolean confirmPasswordReset(@Argument String token, @Argument String newPassword) {
        PasswordResetConfirmDto dto = new PasswordResetConfirmDto();
        dto.setToken(token);
        dto.setNewPassword(newPassword);
        authService.confirmPasswordReset(dto);
        return true;
    }

    @MutationMapping
    public boolean changePassword(@Argument String currentPassword, @Argument String newPassword) {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword(currentPassword);
        request.setNewPassword(newPassword);
        authService.changePassword(SecurityUtil.getCurrentUserId(), request);
        return true;
    }

    @MutationMapping
    public boolean deleteAccount() {
        authService.deleteAccount(SecurityUtil.getCurrentUserId());
        return true;
    }

    private HttpServletRequest httpRequest() {
        return ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
    }
}
