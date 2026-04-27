package com.auction.auctionservice.controller;

import com.auction.auctionservice.dto.request.CreateLotRequest;
import com.auction.auctionservice.dto.request.UpdateLotRequest;
import com.auction.auctionservice.dto.response.LotResponse;
import com.auction.auctionservice.entity.LotStatusCode;
import com.auction.auctionservice.exception.GlobalExceptionHandler;
import com.auction.auctionservice.service.LotService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LotController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class LotControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private LotService lotService;

    private LotResponse lotResponse;

    @BeforeEach
    void setUp() {
        objectMapper.registerModule(new JavaTimeModule());
        lotResponse = new LotResponse(
                1L,
                10L,
                2L,
                "Books",
                LotStatusCode.DRAFT,
                "Clean Code",
                "Book",
                BigDecimal.valueOf(100),
                BigDecimal.valueOf(100),
                BigDecimal.TEN,
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(1),
                null,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }

    @Test
    void shouldSearchLots() throws Exception {
        when(lotService.search(any(), any())).thenReturn(List.of(lotResponse));

        mockMvc.perform(get("/lots").param("status", "ACTIVE").param("categoryId", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].title").value("Clean Code"));
    }

    @Test
    void shouldGetLotById() throws Exception {
        when(lotService.getById(anyLong())).thenReturn(lotResponse);

        mockMvc.perform(get("/lots/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void shouldCreateLot() throws Exception {
        CreateLotRequest request = new CreateLotRequest(
                "Clean Code",
                "Book",
                2L,
                BigDecimal.valueOf(100),
                BigDecimal.TEN,
                LocalDateTime.now().plusDays(1)
        );
        when(lotService.create(any(CreateLotRequest.class))).thenReturn(lotResponse);

        mockMvc.perform(post("/lots")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Clean Code"));
    }

    @Test
    void shouldRejectInvalidLotPayload() throws Exception {
        CreateLotRequest request = new CreateLotRequest(
                "",
                null,
                null,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                LocalDateTime.now().minusDays(1)
        );

        mockMvc.perform(post("/lots")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.fieldErrors.title").exists())
                .andExpect(jsonPath("$.fieldErrors.categoryId").exists())
                .andExpect(jsonPath("$.fieldErrors.startPrice").exists())
                .andExpect(jsonPath("$.fieldErrors.bidStep").exists())
                .andExpect(jsonPath("$.fieldErrors.endTime").exists());
    }

    @Test
    void shouldUpdateLot() throws Exception {
        when(lotService.update(anyLong(), any(UpdateLotRequest.class))).thenReturn(lotResponse);

        mockMvc.perform(put("/lots/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateLotRequest("Clean Code 2", null, null, null, null, null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void shouldActivateLot() throws Exception {
        when(lotService.activate(anyLong())).thenReturn(lotResponse);

        mockMvc.perform(post("/lots/1/activate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void shouldCancelLot() throws Exception {
        when(lotService.cancel(anyLong(), anyString())).thenReturn(lotResponse);

        mockMvc.perform(post("/lots/1/cancel").param("comment", "bad lot"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));

        verify(lotService).cancel(1L, "bad lot");
    }
}
