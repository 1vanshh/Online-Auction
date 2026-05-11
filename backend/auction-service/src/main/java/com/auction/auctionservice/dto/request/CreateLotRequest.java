package com.auction.auctionservice.dto.request;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CreateLotRequest(@NotBlank @Size(min = 3, max = 255) String title, String description,
                               @NotNull Long categoryId, @NotNull @Positive BigDecimal startPrice,
                               @NotNull @Positive BigDecimal bidStep, @NotNull @Future LocalDateTime endTime,
                               @Size(max = 500) String mainImageUrl) {
    public CreateLotRequest(@NotBlank @Size(min = 3, max = 255) String title, String description,
                            @NotNull Long categoryId, @NotNull @Positive BigDecimal startPrice,
                            @NotNull @Positive BigDecimal bidStep, @NotNull @Future LocalDateTime endTime) {
        this(title, description, categoryId, startPrice, bidStep, endTime, null);
    }
}
