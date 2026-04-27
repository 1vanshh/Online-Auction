package com.auction.auctionservice.mapper;

import com.auction.auctionservice.dto.response.*;
import com.auction.auctionservice.entity.*;
import org.springframework.stereotype.Component;

@Component
public class AuctionMapper {
    public CategoryResponse toCategoryResponse(Category c) {
        return new CategoryResponse(c.getId(), c.getName(), c.getDescription(), c.getCreatedAt());
    }

    public LotResponse toLotResponse(Lot l) {
        return new LotResponse(l.getId(), l.getSellerId(), l.getCategory().getId(), l.getCategory().getName(), l.getStatus().getCode(), l.getTitle(), l.getDescription(), l.getStartPrice(), l.getCurrentPrice(), l.getBidStep(), l.getStartTime(), l.getEndTime(), l.getWinnerId(), l.getCreatedAt(), l.getUpdatedAt());
    }
}
