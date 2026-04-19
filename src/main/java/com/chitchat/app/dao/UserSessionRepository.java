package com.chitchat.app.dao;

import com.chitchat.app.entity.UserSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserSessionRepository extends JpaRepository<UserSession, Long> {

    Optional<UserSession> findByTokenHashAndRevokedFalse(String tokenHash);

    List<UserSession> findByUserIdAndRevokedFalse(Long userId);
}
