package com.chitchat.app.dao;

import com.chitchat.app.entity.Room;
import com.chitchat.app.entity.enums.RoomVisibility;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RoomRepository extends JpaRepository<Room, Long> {

    Optional<Room> findByName(String name);

    boolean existsByName(String name);

    Page<Room> findByVisibilityAndNameContainingIgnoreCase(RoomVisibility visibility, String name, Pageable pageable);

    List<Room> findByOwnerId(Long ownerId);
}
