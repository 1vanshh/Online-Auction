package com.auction.biddingservice.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

record LotSnapshot(
        Long id,
        Long sellerId,
        String status,
        BigDecimal currentPrice,
        BigDecimal bidStep,
        LocalDateTime endTime
) {
}
