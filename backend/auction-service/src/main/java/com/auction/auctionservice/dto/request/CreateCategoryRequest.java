package com.auction.auctionservice.dto.request;

import jakarta.validation.constraints.*;

public record CreateCategoryRequest(@NotBlank @Size(min = 2, max = 100) String name,
                                    @Size(max = 255) String description) {
}
