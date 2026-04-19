package com.chitchat.app.dao;

import com.chitchat.app.entity.UserBan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserBanRepository extends JpaRepository<UserBan, Long> {

    boolean existsByBannerIdAndBannedId(Long bannerId, Long bannedId);

    Optional<UserBan> findByBannerIdAndBannedId(Long bannerId, Long bannedId);

    void deleteByBannerIdAndBannedId(Long bannerId, Long bannedId);

    @Modifying
    @Query("DELETE FROM UserBan ub WHERE ub.banner.id = :userId OR ub.banned.id = :userId")
    void deleteAllByBannerIdOrBannedId(@Param("userId") Long userId);
}
