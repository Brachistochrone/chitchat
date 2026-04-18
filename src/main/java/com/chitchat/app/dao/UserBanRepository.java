package com.chitchat.app.dao;

import com.chitchat.app.entity.UserBan;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserBanRepository extends JpaRepository<UserBan, Long> {

    boolean existsByBannerIdAndBannedId(Long bannerId, Long bannedId);
}
