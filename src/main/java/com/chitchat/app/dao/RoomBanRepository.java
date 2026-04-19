package com.chitchat.app.dao;

import com.chitchat.app.entity.RoomBan;
import com.chitchat.app.entity.RoomBanId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RoomBanRepository extends JpaRepository<RoomBan, RoomBanId> {

    List<RoomBan> findByIdRoomId(Long roomId);

    boolean existsByIdRoomIdAndIdUserId(Long roomId, Long userId);

    Optional<RoomBan> findByIdRoomIdAndIdUserId(Long roomId, Long userId);

    void deleteByIdUserId(Long userId);
}
