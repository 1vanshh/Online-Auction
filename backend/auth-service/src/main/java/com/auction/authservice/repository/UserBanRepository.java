package com.auction.authservice.repository;

import com.auction.authservice.entity.User;
import com.auction.authservice.entity.UserBan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UserBanRepository extends JpaRepository<UserBan, Long> {

    Optional<UserBan> findFirstByUserAndActiveTrueAndBannedUntilAfterOrderByBannedUntilDesc(User user, LocalDateTime now);

    List<UserBan> findAllByUserAndActiveTrue(User user);
}
