package com.auction.authservice.service;

import com.auction.authservice.entity.User;
import com.auction.authservice.entity.UserBan;
import com.auction.authservice.repository.UserBanRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class UserBanService {

    public static final String DEFAULT_ADMIN_BAN_REASON = "Blocked by administrator";
    public static final int DEFAULT_ADMIN_BAN_DAYS = 30;

    private final UserBanRepository userBanRepository;

    public UserBanService(UserBanRepository userBanRepository) {
        this.userBanRepository = userBanRepository;
    }

    @Transactional(readOnly = true)
    public Optional<UserBan> getActiveBan(User user) {
        return userBanRepository.findFirstByUserAndActiveTrueAndBannedUntilAfterOrderByBannedUntilDesc(user, LocalDateTime.now());
    }

    @Transactional
    public boolean syncBanStatus(User user) {
        Optional<UserBan> activeBan = getActiveBan(user);
        boolean bannedNow = activeBan.isPresent();
        user.setBanned(bannedNow);
        if (bannedNow) {
            user.setActive(false);
        }
        return bannedNow;
    }

    @Transactional
    public UserBan createBan(User user, User actor, String reason, LocalDateTime bannedUntil) {
        UserBan ban = new UserBan();
        ban.setUser(user);
        ban.setCreatedBy(actor);
        ban.setReason(reason);
        ban.setBannedUntil(bannedUntil);
        ban.setActive(true);
        user.setBanned(true);
        user.setActive(false);
        return userBanRepository.save(ban);
    }

    @Transactional
    public void revokeAllActiveBans(User user) {
        for (UserBan ban : userBanRepository.findAllByUserAndActiveTrue(user)) {
            ban.setActive(false);
            userBanRepository.save(ban);
        }
        user.setBanned(false);
    }
}
