package com.auction.auctionservice.dto.response;

import com.auction.auctionservice.entity.LotStatusCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record LotResponse(Long id, Long sellerId, Long categoryId, String categoryName, LotStatusCode status,
                          String title, String description, BigDecimal startPrice, BigDecimal currentPrice,
                          BigDecimal bidStep, LocalDateTime startTime, LocalDateTime endTime, Long winnerId,
                          LocalDateTime createdAt, LocalDateTime updatedAt, String sellerName, String winnerName,
                          String mainImageUrl) {
    public LotResponse(Long id, Long sellerId, Long categoryId, String categoryName, LotStatusCode status,
                       String title, String description, BigDecimal startPrice, BigDecimal currentPrice,
                       BigDecimal bidStep, LocalDateTime startTime, LocalDateTime endTime, Long winnerId,
                       LocalDateTime createdAt, LocalDateTime updatedAt) {
        this(id, sellerId, categoryId, categoryName, status, title, description, startPrice, currentPrice, bidStep,
                startTime, endTime, winnerId, createdAt, updatedAt, null, null, null);
    }
}
