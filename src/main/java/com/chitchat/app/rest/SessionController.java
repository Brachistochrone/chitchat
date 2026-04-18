package com.chitchat.app.rest;

import com.chitchat.app.dto.response.SessionResponse;
import com.chitchat.app.security.JwtTokenProvider;
import com.chitchat.app.service.SessionService;
import com.chitchat.app.util.HttpUtil;
import com.chitchat.app.util.SecurityUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Sessions", description = "Session management endpoints")
@RestController
@RequestMapping("/api/sessions")
@RequiredArgsConstructor
public class SessionController {

    private final SessionService sessionService;
    private final JwtTokenProvider jwtTokenProvider;

    @Operation(summary = "List active sessions for the current user")
    @GetMapping
    public ResponseEntity<List<SessionResponse>> getSessions(HttpServletRequest httpRequest) {
        String token = HttpUtil.extractBearerToken(httpRequest);
        String jti = (StringUtils.hasText(token) && jwtTokenProvider.validateToken(token))
                ? jwtTokenProvider.getJtiFromToken(token)
                : null;
        return ResponseEntity.ok(sessionService.getActiveSessions(SecurityUtil.getCurrentUserId(), jti));
    }

    @Operation(summary = "Revoke a specific session")
    @DeleteMapping("/{sessionId}")
    public ResponseEntity<Void> revokeSession(@PathVariable Long sessionId) {
        sessionService.revokeSession(SecurityUtil.getCurrentUserId(), sessionId);
        return ResponseEntity.noContent().build();
    }
}
