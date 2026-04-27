package com.auction.auctionservice.mapper;

import com.auction.auctionservice.entity.*;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class AuctionMapperTest {

    private final AuctionMapper mapper = new AuctionMapper();

    @Test
    void shouldMapCategory() {
        Category category = new Category();
        category.setId(1L);
        category.setName("Electronics");
        category.setDescription("Devices");
        category.setCreatedAt(LocalDateTime.now());

        var response = mapper.toCategoryResponse(category);

        assertEquals(1L, response.id());
        assertEquals("Electronics", response.name());
        assertEquals("Devices", response.description());
        assertSame(category.getCreatedAt(), response.createdAt());
    }

    @Test
    void shouldMapLot() {
        Category category = new Category();
        category.setId(10L);
        category.setName("Books");

        LotStatus status = new LotStatus();
        status.setId(2L);
        status.setCode(LotStatusCode.ACTIVE);

        Lot lot = new Lot();
        lot.setId(3L);
        lot.setSellerId(4L);
        lot.setCategory(category);
        lot.setStatus(status);
        lot.setTitle("Clean Code");
        lot.setDescription("Book");
        lot.setStartPrice(BigDecimal.valueOf(100));
        lot.setCurrentPrice(BigDecimal.valueOf(150));
        lot.setBidStep(BigDecimal.TEN);
        lot.setStartTime(LocalDateTime.now().minusMinutes(1));
        lot.setEndTime(LocalDateTime.now().plusDays(1));
        lot.setWinnerId(5L);
        lot.setCreatedAt(LocalDateTime.now().minusHours(1));
        lot.setUpdatedAt(LocalDateTime.now());

        var response = mapper.toLotResponse(lot);

        assertEquals(3L, response.id());
        assertEquals(4L, response.sellerId());
        assertEquals(10L, response.categoryId());
        assertEquals("Books", response.categoryName());
        assertEquals(LotStatusCode.ACTIVE, response.status());
        assertEquals("Clean Code", response.title());
        assertEquals(BigDecimal.valueOf(150), response.currentPrice());
        assertEquals(5L, response.winnerId());
    }
}
