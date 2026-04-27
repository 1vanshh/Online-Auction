package com.auction.auctionservice.dto.request;

import jakarta.validation.constraints.NotNull;

public record SetWinnerRequest(@NotNull Long winnerId) {
}
