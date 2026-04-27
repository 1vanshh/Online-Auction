package com.auction.auctionservice.dto.request;

import jakarta.validation.constraints.*;

public record UnpaidWinnerBanRequest(@Min(1) @Max(365) int banDays, @Size(max = 255) String reason) {
}
