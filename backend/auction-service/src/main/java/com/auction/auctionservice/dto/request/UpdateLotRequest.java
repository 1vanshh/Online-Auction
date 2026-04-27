package com.auction.auctionservice.dto.request;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record UpdateLotRequest(@Size(min = 3, max = 255) String title, String description, Long categoryId,
                               @Positive BigDecimal startPrice, @Positive BigDecimal bidStep,
                               @Future LocalDateTime endTime) {
}
