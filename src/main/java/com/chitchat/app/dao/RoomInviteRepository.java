package com.chitchat.app.dao;

import com.chitchat.app.entity.RoomInvite;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomInviteRepository extends JpaRepository<RoomInvite, Long> {

    boolean existsByRoomIdAndInvitedUserId(Long roomId, Long invitedUserId);
}
