package com.chitchat.app.dao;

import com.chitchat.app.entity.Contact;
import com.chitchat.app.entity.enums.ContactStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ContactRepository extends JpaRepository<Contact, Long> {

    List<Contact> findByRequesterIdAndStatus(Long requesterId, ContactStatus status);

    List<Contact> findByAddresseeIdAndStatus(Long addresseeId, ContactStatus status);

    Optional<Contact> findByRequesterIdAndAddresseeId(Long requesterId, Long addresseeId);

    boolean existsByRequesterIdAndAddresseeId(Long requesterId, Long addresseeId);

    @Query("SELECT c FROM Contact c WHERE c.status = com.chitchat.app.entity.enums.ContactStatus.ACCEPTED " +
           "AND ((c.requester.id = :userId1 AND c.addressee.id = :userId2) " +
           "  OR (c.requester.id = :userId2 AND c.addressee.id = :userId1))")
    Optional<Contact> findAcceptedBetween(@Param("userId1") Long userId1, @Param("userId2") Long userId2);
}
