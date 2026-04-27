package com.auction.biddingservice.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record BidResultResponse(
        Long id,
        Long lotId,
        Long winnerId,
        BigDecimal finalPrice,
        LocalDateTime paymentDeadline,
        boolean paid,
        LocalDateTime createdAt
) {
}
