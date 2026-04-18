package com.chitchat.app.dao;

import com.chitchat.app.entity.RoomBan;
import com.chitchat.app.entity.RoomBanId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RoomBanRepository extends JpaRepository<RoomBan, RoomBanId> {

    List<RoomBan> findByIdRoomId(Long roomId);

    boolean existsByIdRoomIdAndIdUserId(Long roomId, Long userId);
}
