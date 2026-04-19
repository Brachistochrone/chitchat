package com.chitchat.app.service;

import com.chitchat.app.dto.response.SessionResponse;

import java.util.List;

public interface SessionService {

    List<SessionResponse> getActiveSessions(Long userId, String currentJti);

    void revokeSession(Long userId, Long sessionId);
}
