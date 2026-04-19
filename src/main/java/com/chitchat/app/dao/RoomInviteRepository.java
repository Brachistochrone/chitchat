package com.chitchat.app.dao;

import com.chitchat.app.entity.RoomInvite;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoomInviteRepository extends JpaRepository<RoomInvite, Long> {

    boolean existsByRoomIdAndInvitedUserId(Long roomId, Long invitedUserId);

    Optional<RoomInvite> findByRoomIdAndInvitedUserId(Long roomId, Long invitedUserId);

    void deleteByRoomIdAndInvitedUserId(Long roomId, Long invitedUserId);
}
