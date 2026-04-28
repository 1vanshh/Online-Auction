package com.auction.biddingservice.controller;

import com.auction.biddingservice.dto.request.PlaceBidRequest;
import com.auction.biddingservice.dto.response.BidResponse;
import com.auction.biddingservice.dto.response.BidResultResponse;
import com.auction.biddingservice.service.BiddingService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/bids")
public class BiddingController {
    private final BiddingService service;

    public BiddingController(BiddingService service) {
        this.service = service;
    }

    @PostMapping
    public BidResponse placeBid(@Valid @RequestBody PlaceBidRequest request) {
        return service.placeBid(request);
    }

    @GetMapping("/lots/{lotId}")
    public List<BidResponse> getBidsByLot(@PathVariable("lotId") Long lotId) {
        return service.getBidsByLot(lotId);
    }

    @GetMapping("/my")
    public List<BidResponse> getMyBids() {
        return service.getMyBids();
    }

    @GetMapping("/results/{lotId}")
    public BidResultResponse getResult(@PathVariable("lotId") Long lotId) {
        return service.getResult(lotId);
    }
}
