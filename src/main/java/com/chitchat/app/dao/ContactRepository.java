package com.chitchat.app.dao;

import com.chitchat.app.entity.Contact;
import com.chitchat.app.entity.enums.ContactStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ContactRepository extends JpaRepository<Contact, Long> {

    List<Contact> findByRequesterIdAndStatus(Long requesterId, ContactStatus status);

    List<Contact> findByAddresseeIdAndStatus(Long addresseeId, ContactStatus status);

    Optional<Contact> findByRequesterIdAndAddresseeId(Long requesterId, Long addresseeId);

    boolean existsByRequesterIdAndAddresseeId(Long requesterId, Long addresseeId);
}
