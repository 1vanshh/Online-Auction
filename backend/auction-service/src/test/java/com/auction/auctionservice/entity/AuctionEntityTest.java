package com.auction.auctionservice.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AuctionEntityTest {

    @Test
    void categoryShouldSetCreatedAtOnCreate() {
        Category category = new Category();
        assertNull(category.getCreatedAt());

        category.onCreate();

        assertNotNull(category.getCreatedAt());
    }

    @Test
    void lotShouldSetTimestampsOnCreateAndUpdate() {
        Lot lot = new Lot();
        assertNull(lot.getCreatedAt());
        assertNull(lot.getUpdatedAt());

        lot.onCreate();

        assertNotNull(lot.getCreatedAt());
        assertNotNull(lot.getUpdatedAt());

        var previousUpdatedAt = lot.getUpdatedAt();
        lot.onUpdate();

        assertNotNull(lot.getUpdatedAt());
        assertFalse(lot.getUpdatedAt().isBefore(previousUpdatedAt));
    }

    @Test
    void statusHistoryShouldSetChangedAtOnCreate() {
        LotStatusHistory history = new LotStatusHistory();
        assertNull(history.getChangedAt());

        history.onCreate();

        assertNotNull(history.getChangedAt());
    }
}
