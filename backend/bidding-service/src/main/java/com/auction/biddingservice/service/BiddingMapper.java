package com.auction.biddingservice.service;

import com.auction.biddingservice.dto.response.BidResponse;
import com.auction.biddingservice.dto.response.BidResultResponse;
import com.auction.biddingservice.entity.Bid;
import com.auction.biddingservice.entity.BidResult;
import org.springframework.stereotype.Component;

@Component
public class BiddingMapper {
    public BidResponse toBidResponse(Bid bid) {
        return new BidResponse(bid.getId(), bid.getLotId(), bid.getBidderId(), bid.getAmount(), bid.getCreatedAt());
    }
    public BidResultResponse toResultResponse(BidResult result) {
        return new BidResultResponse(result.getId(), result.getLotId(), result.getWinnerId(), result.getFinalPrice(), result.getPaymentDeadline(), result.isPaid(), result.getCreatedAt());
    }
}
