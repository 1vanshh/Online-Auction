package com.auction.auctionservice.controller;

import com.auction.auctionservice.dto.request.CreateCategoryRequest;
import com.auction.auctionservice.dto.response.*;
import com.auction.auctionservice.service.CategoryService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/categories")
public class CategoryController {
    private final CategoryService service;

    public CategoryController(CategoryService service) {
        this.service = service;
    }

    @GetMapping
    public List<CategoryResponse> getAll() {
        return service.getAll();
    }

    @PostMapping("/admin")
    public CategoryResponse create(@Valid @RequestBody CreateCategoryRequest request) {
        return service.create(request);
    }

    @PutMapping("/admin/{id}")
    public CategoryResponse update(@PathVariable Long id, @Valid @RequestBody CreateCategoryRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/admin/{id}")
    public MessageResponse delete(@PathVariable Long id) {
        service.delete(id);
        return new MessageResponse("Category deleted");
    }
}
