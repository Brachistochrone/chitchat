package com.chitchat.app.service;

import com.chitchat.app.dao.RoomRepository;
import com.chitchat.app.dao.UserRepository;
import com.chitchat.app.entity.Room;
import com.chitchat.app.entity.User;
import com.chitchat.app.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EntityLoaderService {

    private final RoomRepository roomRepository;
    private final UserRepository userRepository;

    @Cacheable(value = "rooms", key = "#roomId")
    public Room loadRoom(Long roomId) {
        return roomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found"));
    }

    @Cacheable(value = "users", key = "#userId")
    public User loadActiveUser(Long userId) {
        return userRepository.findById(userId)
                .filter(u -> u.getDeletedAt() == null)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
