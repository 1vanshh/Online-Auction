package com.auction.auctionservice.controller;

import com.auction.auctionservice.dto.request.CreateCategoryRequest;
import com.auction.auctionservice.dto.response.CategoryResponse;
import com.auction.auctionservice.dto.response.MessageResponse;
import com.auction.auctionservice.service.CategoryService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
    public CategoryResponse update(@PathVariable("id") Long id, @Valid @RequestBody CreateCategoryRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/admin/{id}")
    public MessageResponse delete(@PathVariable("id") Long id) {
        service.delete(id);
        return new MessageResponse("Category deleted");
    }
}
