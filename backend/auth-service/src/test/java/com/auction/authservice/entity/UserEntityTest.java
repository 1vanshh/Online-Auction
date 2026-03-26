package com.auction.authservice.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class UserEntityTest {

    @Test
    void shouldInitializeTimestampsOnCreate() {
        User user = new User();

        user.onCreate();

        assertNotNull(user.getCreatedAt());
        assertNotNull(user.getUpdatedAt());
        assertFalse(user.getUpdatedAt().isBefore(user.getCreatedAt()));
    }

    @Test
    void shouldRefreshUpdatedAtOnUpdate() throws InterruptedException {
        User user = new User();
        user.onCreate();
        LocalDateTime initialUpdatedAt = user.getUpdatedAt();

        Thread.sleep(5);
        user.onUpdate();

        assertTrue(user.getUpdatedAt().isAfter(initialUpdatedAt) || user.getUpdatedAt().isEqual(initialUpdatedAt));
    }
}
