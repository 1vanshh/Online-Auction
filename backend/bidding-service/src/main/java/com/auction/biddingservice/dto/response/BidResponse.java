package com.auction.biddingservice.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record BidResponse(
        Long id,
        Long lotId,
        Long bidderId,
        BigDecimal amount,
        LocalDateTime createdAt
) {
}
