package com.chitchat.app.service;

import com.chitchat.app.dao.UserSessionRepository;
import com.chitchat.app.dto.response.SessionResponse;
import com.chitchat.app.entity.UserSession;
import com.chitchat.app.exception.ForbiddenException;
import com.chitchat.app.exception.ResourceNotFoundException;
import com.chitchat.app.util.EntityMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SessionServiceImpl implements SessionService {

    private final UserSessionRepository userSessionRepository;

    @Override
    public List<SessionResponse> getActiveSessions(Long userId, String currentJti) {
        return userSessionRepository.findByUserIdAndRevokedFalse(userId).stream()
                .map(session -> EntityMapper.toSessionResponse(session, currentJti))
                .toList();
    }

    @Override
    @Transactional
    public void revokeSession(Long userId, Long sessionId) {
        UserSession session = userSessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found"));

        if (!session.getUser().getId().equals(userId)) {
            throw new ForbiddenException("Cannot revoke another user's session");
        }

        session.setRevoked(true);
        userSessionRepository.save(session);
    }
}
