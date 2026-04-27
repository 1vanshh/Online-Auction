package com.auction.biddingservice.controller;

import com.auction.biddingservice.dto.request.FinishLotBiddingRequest;
import com.auction.biddingservice.dto.response.BidResultResponse;
import com.auction.biddingservice.dto.response.MessageResponse;
import com.auction.biddingservice.service.BiddingService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/bids")
public class AdminBiddingController {
    private final BiddingService service;

    public AdminBiddingController(BiddingService service) {
        this.service = service;
    }

    @PostMapping("/lots/{lotId}/finish")
    public BidResultResponse finishLotBidding(@PathVariable("lotId") Long lotId, @Valid @RequestBody FinishLotBiddingRequest request) {
        return service.finishLotBidding(lotId, request);
    }

    @PostMapping("/lots/{lotId}/paid")
    public BidResultResponse markPaid(@PathVariable("lotId") Long lotId) {
        return service.markPaid(lotId);
    }

    @PostMapping("/health-check")
    public MessageResponse healthCheck() {
        return new MessageResponse("Bidding admin endpoint is available");
    }
}
