package com.auction.auctionservice.service;

import com.auction.auctionservice.dto.request.CreateCategoryRequest;
import com.auction.auctionservice.dto.response.CategoryResponse;
import com.auction.auctionservice.entity.Category;
import com.auction.auctionservice.exception.BadRequestException;
import com.auction.auctionservice.exception.NotFoundException;
import com.auction.auctionservice.mapper.AuctionMapper;
import com.auction.auctionservice.repository.CategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository repo;
    @Mock
    private AuctionMapper mapper;

    @InjectMocks
    private CategoryService service;

    private Category category;
    private CategoryResponse response;

    @BeforeEach
    void setUp() {
        category = new Category();
        category.setId(1L);
        category.setName("Books");
        category.setDescription("Printed materials");
        category.setCreatedAt(LocalDateTime.now());

        response = new CategoryResponse(1L, "Books", "Printed materials", category.getCreatedAt());
    }

    @Test
    void shouldReturnAllCategories() {
        when(repo.findAll()).thenReturn(List.of(category));
        when(mapper.toCategoryResponse(category)).thenReturn(response);

        List<CategoryResponse> result = service.getAll();

        assertEquals(1, result.size());
        assertSame(response, result.getFirst());
    }

    @Test
    void shouldCreateCategory() {
        when(repo.existsByNameIgnoreCase("Books")).thenReturn(false);
        when(repo.save(any(Category.class))).thenAnswer(invocation -> {
            Category saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });
        when(mapper.toCategoryResponse(any(Category.class))).thenReturn(response);

        CategoryResponse result = service.create(new CreateCategoryRequest(" Books ", " Printed materials "));

        assertSame(response, result);
        verify(repo).save(argThat(c -> c.getName().equals("Books") && c.getDescription().equals("Printed materials")));
    }

    @Test
    void shouldRejectDuplicateCategory() {
        when(repo.existsByNameIgnoreCase("Books")).thenReturn(true);

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> service.create(new CreateCategoryRequest("Books", null)));

        assertEquals("Category already exists", ex.getMessage());
        verify(repo, never()).save(any());
    }

    @Test
    void shouldUpdateCategory() {
        when(repo.findById(1L)).thenReturn(Optional.of(category));
        when(repo.findByNameIgnoreCase("Electronics")).thenReturn(Optional.empty());
        when(mapper.toCategoryResponse(category)).thenReturn(new CategoryResponse(1L, "Electronics", "Devices", category.getCreatedAt()));

        CategoryResponse result = service.update(1L, new CreateCategoryRequest("Electronics", "Devices"));

        assertEquals("Electronics", result.name());
        assertEquals("Electronics", category.getName());
        assertEquals("Devices", category.getDescription());
    }

    @Test
    void shouldThrowWhenCategoryNotFoundOnUpdate() {
        when(repo.findById(404L)).thenReturn(Optional.empty());

        NotFoundException ex = assertThrows(NotFoundException.class,
                () -> service.update(404L, new CreateCategoryRequest("Books", null)));

        assertEquals("Category not found", ex.getMessage());
    }

    @Test
    void shouldDeleteCategory() {
        when(repo.existsById(1L)).thenReturn(true);

        service.delete(1L);

        verify(repo).deleteById(1L);
    }

    @Test
    void shouldThrowWhenCategoryNotFoundOnDelete() {
        when(repo.existsById(404L)).thenReturn(false);

        NotFoundException ex = assertThrows(NotFoundException.class, () -> service.delete(404L));

        assertEquals("Category not found", ex.getMessage());
        verify(repo, never()).deleteById(anyLong());
    }
}
