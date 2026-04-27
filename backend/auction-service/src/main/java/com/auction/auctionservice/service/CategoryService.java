package com.auction.auctionservice.service;

import com.auction.auctionservice.dto.request.CreateCategoryRequest;
import com.auction.auctionservice.dto.response.CategoryResponse;
import com.auction.auctionservice.entity.Category;
import com.auction.auctionservice.exception.*;
import com.auction.auctionservice.mapper.AuctionMapper;
import com.auction.auctionservice.repository.CategoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CategoryService {
    private final CategoryRepository repo;
    private final AuctionMapper mapper;

    public CategoryService(CategoryRepository repo, AuctionMapper mapper) {
        this.repo = repo;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> getAll() {
        return repo.findAll().stream().map(mapper::toCategoryResponse).toList();
    }

    @Transactional
    public CategoryResponse create(CreateCategoryRequest r) {
        String name = req(r.name(), "Category name is required");
        if (repo.existsByNameIgnoreCase(name)) throw new BadRequestException("Category already exists");
        Category c = new Category();
        c.setName(name);
        c.setDescription(trim(r.description()));
        return mapper.toCategoryResponse(repo.save(c));
    }

    @Transactional
    public CategoryResponse update(Long id, CreateCategoryRequest r) {
        Category c = repo.findById(id).orElseThrow(() -> new NotFoundException("Category not found"));
        String name = req(r.name(), "Category name is required");
        repo.findByNameIgnoreCase(name).filter(e -> !e.getId().equals(id)).ifPresent(e -> {
            throw new BadRequestException("Category already exists");
        });
        c.setName(name);
        c.setDescription(trim(r.description()));
        return mapper.toCategoryResponse(c);
    }

    @Transactional
    public void delete(Long id) {
        if (!repo.existsById(id)) throw new NotFoundException("Category not found");
        repo.deleteById(id);
    }

    private String trim(String v) {
        if (v == null) return null;
        String t = v.trim();
        return t.isEmpty() ? null : t;
    }

    private String req(String v, String m) {
        String t = trim(v);
        if (t == null) throw new BadRequestException(m);
        return t;
    }
}
