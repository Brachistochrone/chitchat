package com.chitchat.app.service;

import com.chitchat.app.dao.UserSessionRepository;
import com.chitchat.app.dto.response.SessionResponse;
import com.chitchat.app.entity.User;
import com.chitchat.app.entity.UserSession;
import com.chitchat.app.exception.ForbiddenException;
import com.chitchat.app.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SessionServiceImplTest {

    @Mock private UserSessionRepository userSessionRepository;

    @InjectMocks
    private SessionServiceImpl sessionService;

    private User testUser;
    private UserSession testSession;

    @BeforeEach
    void setUp() {
        testUser = User.builder().id(1L).username("alice").build();
        testSession = UserSession.builder()
                .id(10L)
                .user(testUser)
                .tokenHash("current-jti")
                .browser("Chrome")
                .ipAddress("127.0.0.1")
                .lastSeenAt(OffsetDateTime.now())
                .revoked(false)
                .build();
    }

    @Test
    void getActiveSessions_returnsNonRevoked() {
        when(userSessionRepository.findByUserIdAndRevokedFalse(1L))
                .thenReturn(List.of(testSession));

        List<SessionResponse> sessions = sessionService.getActiveSessions(1L, "current-jti");

        assertThat(sessions).hasSize(1);
        assertThat(sessions.get(0).isCurrent()).isTrue();
        assertThat(sessions.get(0).getBrowser()).isEqualTo("Chrome");
    }

    @Test
    void getActiveSessions_marksCurrentCorrectly() {
        UserSession otherSession = UserSession.builder()
                .id(11L)
                .user(testUser)
                .tokenHash("other-jti")
                .lastSeenAt(OffsetDateTime.now())
                .revoked(false)
                .build();

        when(userSessionRepository.findByUserIdAndRevokedFalse(1L))
                .thenReturn(List.of(testSession, otherSession));

        List<SessionResponse> sessions = sessionService.getActiveSessions(1L, "current-jti");

        assertThat(sessions.stream().filter(SessionResponse::isCurrent).count()).isEqualTo(1);
        assertThat(sessions.stream().filter(s -> !s.isCurrent()).count()).isEqualTo(1);
    }

    @Test
    void revokeSession_success() {
        when(userSessionRepository.findById(10L)).thenReturn(Optional.of(testSession));
        when(userSessionRepository.save(any(UserSession.class))).thenAnswer(i -> i.getArgument(0));

        sessionService.revokeSession(1L, 10L);

        assertThat(testSession.isRevoked()).isTrue();
        verify(userSessionRepository).save(testSession);
    }

    @Test
    void revokeSession_sessionNotFound_throwsResourceNotFound() {
        when(userSessionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sessionService.revokeSession(1L, 99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void revokeSession_wrongOwner_throwsForbidden() {
        when(userSessionRepository.findById(10L)).thenReturn(Optional.of(testSession));

        assertThatThrownBy(() -> sessionService.revokeSession(99L, 10L))
                .isInstanceOf(ForbiddenException.class);
    }
}
