package com.auction.biddingservice.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record PlaceBidRequest(
        @NotNull(message = "Lot id is required") Long lotId,
        @NotNull(message = "Amount is required") @Positive(message = "Amount must be positive") BigDecimal amount
) {
}
