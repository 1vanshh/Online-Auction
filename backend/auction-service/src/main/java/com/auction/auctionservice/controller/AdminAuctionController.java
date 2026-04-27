package com.auction.auctionservice.controller;

import com.auction.auctionservice.dto.request.*;
import com.auction.auctionservice.dto.response.*;
import com.auction.auctionservice.service.LotService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/lots")
public class AdminAuctionController {
    private final LotService service;

    public AdminAuctionController(LotService service) {
        this.service = service;
    }

    @PostMapping("/{id}/finish")
    public LotResponse finish(@PathVariable Long id) {
        return service.finish(id);
    }

    @PostMapping("/{id}/winner")
    public LotResponse setWinner(@PathVariable Long id, @Valid @RequestBody SetWinnerRequest request) {
        return service.setWinner(id, request);
    }

    @PostMapping("/{id}/unpaid-ban")
    public MessageResponse banUnpaidWinner(@PathVariable Long id, @Valid @RequestBody UnpaidWinnerBanRequest request) {
        service.banUnpaidWinner(id, request);
        return new MessageResponse("Winner banned for non-payment");
    }
}
