package com.auction.auctionservice.controller;

import com.auction.auctionservice.dto.request.SetWinnerRequest;
import com.auction.auctionservice.dto.request.UnpaidWinnerBanRequest;
import com.auction.auctionservice.dto.response.LotResponse;
import com.auction.auctionservice.dto.response.MessageResponse;
import com.auction.auctionservice.service.LotService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/lots")
public class AdminAuctionController {
    private final LotService service;

    public AdminAuctionController(LotService service) {
        this.service = service;
    }

    @PostMapping("/{id}/finish")
    public LotResponse finish(@PathVariable("id") Long id) {
        return service.finish(id);
    }

    @PostMapping("/{id}/winner")
    public LotResponse setWinner(@PathVariable("id") Long id, @Valid @RequestBody SetWinnerRequest request) {
        return service.setWinner(id, request);
    }

    @PostMapping("/{id}/unpaid-ban")
    public MessageResponse banUnpaidWinner(@PathVariable("id") Long id, @Valid @RequestBody UnpaidWinnerBanRequest request) {
        service.banUnpaidWinner(id, request);
        return new MessageResponse("Winner banned for non-payment");
    }
}
