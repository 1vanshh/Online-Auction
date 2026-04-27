package com.auction.auctionservice.controller;

import com.auction.auctionservice.dto.request.CreateCategoryRequest;
import com.auction.auctionservice.dto.response.CategoryResponse;
import com.auction.auctionservice.exception.GlobalExceptionHandler;
import com.auction.auctionservice.service.CategoryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CategoryController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class CategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CategoryService categoryService;

    @Test
    void shouldReturnCategories() throws Exception {
        when(categoryService.getAll())
                .thenReturn(List.of(new CategoryResponse(1L, "Books", "Printed", LocalDateTime.now())));

        mockMvc.perform(get("/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Books"));
    }

    @Test
    void shouldCreateCategory() throws Exception {
        CreateCategoryRequest request = new CreateCategoryRequest("Books", "Printed");
        when(categoryService.create(any(CreateCategoryRequest.class)))
                .thenReturn(new CategoryResponse(1L, "Books", "Printed", LocalDateTime.now()));

        mockMvc.perform(post("/categories/admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Books"));

        verify(categoryService).create(any(CreateCategoryRequest.class));
    }

    @Test
    void shouldRejectInvalidCategory() throws Exception {
        CreateCategoryRequest request = new CreateCategoryRequest("", "Printed");

        mockMvc.perform(post("/categories/admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.fieldErrors.name").exists());
    }

    @Test
    void shouldUpdateCategory() throws Exception {
        CreateCategoryRequest request = new CreateCategoryRequest("Electronics", "Devices");
        when(categoryService.update(anyLong(), any(CreateCategoryRequest.class)))
                .thenReturn(new CategoryResponse(1L, "Electronics", "Devices", LocalDateTime.now()));

        mockMvc.perform(put("/categories/admin/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Electronics"));
    }

    @Test
    void shouldDeleteCategory() throws Exception {
        mockMvc.perform(delete("/categories/admin/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Category deleted"));

        verify(categoryService).delete(1L);
    }
}
