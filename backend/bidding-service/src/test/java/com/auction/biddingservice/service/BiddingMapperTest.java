package com.auction.biddingservice.service;

import com.auction.biddingservice.dto.response.BidResponse;
import com.auction.biddingservice.dto.response.BidResultResponse;
import com.auction.biddingservice.entity.Bid;
import com.auction.biddingservice.entity.BidResult;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class BiddingMapperTest {
    private final BiddingMapper mapper = new BiddingMapper();

    @Test
    void shouldMapBidToResponse() {
        LocalDateTime now = LocalDateTime.now();
        Bid bid = new Bid();
        bid.setId(1L);
        bid.setLotId(10L);
        bid.setBidderId(20L);
        bid.setAmount(BigDecimal.valueOf(120));
        bid.setCreatedAt(now);

        BidResponse response = mapper.toBidResponse(bid);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.lotId()).isEqualTo(10L);
        assertThat(response.bidderId()).isEqualTo(20L);
        assertThat(response.amount()).isEqualByComparingTo("120");
        assertThat(response.createdAt()).isEqualTo(now);
    }

    @Test
    void shouldMapBidResultToResponse() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime deadline = now.plusDays(3);
        BidResult result = new BidResult();
        result.setId(1L);
        result.setLotId(10L);
        result.setWinnerId(20L);
        result.setFinalPrice(BigDecimal.valueOf(150));
        result.setPaymentDeadline(deadline);
        result.setPaid(true);
        result.setCreatedAt(now);

        BidResultResponse response = mapper.toResultResponse(result);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.lotId()).isEqualTo(10L);
        assertThat(response.winnerId()).isEqualTo(20L);
        assertThat(response.finalPrice()).isEqualByComparingTo("150");
        assertThat(response.paymentDeadline()).isEqualTo(deadline);
        assertThat(response.paid()).isTrue();
        assertThat(response.createdAt()).isEqualTo(now);
    }
}
