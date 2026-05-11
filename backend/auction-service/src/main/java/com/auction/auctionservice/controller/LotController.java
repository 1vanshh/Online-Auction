package com.auction.auctionservice.controller;

import com.auction.auctionservice.dto.request.CreateLotRequest;
import com.auction.auctionservice.dto.request.UpdateLotRequest;
import com.auction.auctionservice.dto.response.ImageUploadResponse;
import com.auction.auctionservice.dto.response.LotResponse;
import com.auction.auctionservice.entity.LotStatusCode;
import com.auction.auctionservice.service.LotImageStorageService;
import com.auction.auctionservice.service.LotService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/lots")
public class LotController {
    private final LotService service;
    private final LotImageStorageService imageStorage;

    public LotController(LotService service, LotImageStorageService imageStorage) {
        this.service = service;
        this.imageStorage = imageStorage;
    }

    @GetMapping
    public List<LotResponse> search(
            @RequestParam(name = "status", required = false) LotStatusCode status,
            @RequestParam(name = "categoryId", required = false) Long categoryId
    ) {
        return service.search(status, categoryId);
    }

    @GetMapping("/{id}")
    public LotResponse getById(@PathVariable("id") Long id) {
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

    @PostMapping(value = "/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ImageUploadResponse uploadImage(@RequestParam("image") MultipartFile image) {
        return imageStorage.store(image);
    }

    @PutMapping("/{id}")
    public LotResponse update(@PathVariable("id") Long id, @Valid @RequestBody UpdateLotRequest request) {
        return service.update(id, request);
    }

    @PostMapping("/{id}/activate")
    public LotResponse activate(@PathVariable("id") Long id) {
        return service.activate(id);
    }

    @PostMapping("/{id}/cancel")
    public LotResponse cancel(
            @PathVariable("id") Long id,
            @RequestParam(name = "comment", required = false) String comment
    ) {
        return service.cancel(id, comment);
    }
}
