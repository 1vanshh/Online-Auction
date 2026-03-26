package com.auction.authservice.service;

import com.auction.authservice.entity.User;
import com.auction.authservice.entity.UserBan;
import com.auction.authservice.repository.UserBanRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserBanServiceTest {

    @Mock
    private UserBanRepository userBanRepository;

    @InjectMocks
    private UserBanService userBanService;

    @Test
    void shouldMarkUserBannedWhenActiveBanExists() {
        User user = new User();
        user.setActive(true);
        user.setBanned(false);

        UserBan ban = new UserBan();
        ban.setUser(user);
        ban.setActive(true);
        ban.setBannedUntil(LocalDateTime.now().plusDays(1));

        when(userBanRepository.findFirstByUserAndActiveTrueAndBannedUntilAfterOrderByBannedUntilDesc(eq(user), any()))
                .thenReturn(Optional.of(ban));

        boolean banned = userBanService.syncBanStatus(user);

        assertTrue(banned);
        assertTrue(user.isBanned());
        assertFalse(user.isActive());
    }

    @Test
    void shouldClearBanFlagWhenNoActiveBanExists() {
        User user = new User();
        user.setActive(true);
        user.setBanned(true);

        when(userBanRepository.findFirstByUserAndActiveTrueAndBannedUntilAfterOrderByBannedUntilDesc(eq(user), any()))
                .thenReturn(Optional.empty());

        boolean banned = userBanService.syncBanStatus(user);

        assertFalse(banned);
        assertFalse(user.isBanned());
    }

    @Test
    void shouldRevokeAllActiveBans() {
        User user = new User();
        user.setBanned(true);

        UserBan first = new UserBan();
        first.setActive(true);
        UserBan second = new UserBan();
        second.setActive(true);

        when(userBanRepository.findAllByUserAndActiveTrue(user)).thenReturn(List.of(first, second));

        userBanService.revokeAllActiveBans(user);

        assertFalse(first.isActive());
        assertFalse(second.isActive());
        assertFalse(user.isBanned());
        verify(userBanRepository, times(2)).save(any(UserBan.class));
    }
}
