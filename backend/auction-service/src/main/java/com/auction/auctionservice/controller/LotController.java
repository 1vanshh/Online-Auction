package com.auction.auctionservice.controller;

import com.auction.auctionservice.dto.request.*;
import com.auction.auctionservice.dto.response.LotResponse;
import com.auction.auctionservice.entity.LotStatusCode;
import com.auction.auctionservice.service.LotService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/lots")
public class LotController {
    private final LotService service;

    public LotController(LotService service) {
        this.service = service;
    }

    @GetMapping
    public List<LotResponse> search(@RequestParam(required = false) LotStatusCode status, @RequestParam(required = false) Long categoryId) {
        return service.search(status, categoryId);
    }

    @GetMapping("/{id}")
    public LotResponse getById(@PathVariable Long id) {
        return service.getById(id);
    }

    @GetMapping("/my")
    public List<LotResponse> getMyLots() {
        return service.getMyLots();
    }

    @PostMapping
    public LotResponse create(@Valid @RequestBody CreateLotRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    public LotResponse update(@PathVariable Long id, @Valid @RequestBody UpdateLotRequest request) {
        return service.update(id, request);
    }

    @PostMapping("/{id}/activate")
    public LotResponse activate(@PathVariable Long id) {
        return service.activate(id);
    }

    @PostMapping("/{id}/cancel")
    public LotResponse cancel(@PathVariable Long id, @RequestParam(required = false) String comment) {
        return service.cancel(id, comment);
    }
}
