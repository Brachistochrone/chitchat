package com.chitchat.app.dao;

import com.chitchat.app.entity.RoomMember;
import com.chitchat.app.entity.RoomMemberId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RoomMemberRepository extends JpaRepository<RoomMember, RoomMemberId> {

    List<RoomMember> findByIdRoomId(Long roomId);

    List<RoomMember> findByIdUserId(Long userId);

    boolean existsByIdRoomIdAndIdUserId(Long roomId, Long userId);
}
