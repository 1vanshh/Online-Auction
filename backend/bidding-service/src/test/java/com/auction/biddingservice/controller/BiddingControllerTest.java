package com.auction.biddingservice.controller;

import com.auction.biddingservice.dto.request.PlaceBidRequest;
import com.auction.biddingservice.dto.response.BidResponse;
import com.auction.biddingservice.dto.response.BidResultResponse;
import com.auction.biddingservice.exception.GlobalExceptionHandler;
import com.auction.biddingservice.service.BiddingService;
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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BiddingController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class BiddingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private BiddingService biddingService;

    private BidResponse bidResponse;
    private BidResultResponse resultResponse;

    @BeforeEach
    void setUp() {
        objectMapper.registerModule(new JavaTimeModule());
        bidResponse = new BidResponse(1L, 10L, 20L, BigDecimal.valueOf(120), LocalDateTime.now());
        resultResponse = new BidResultResponse(1L, 10L, 20L, BigDecimal.valueOf(120), LocalDateTime.now().plusDays(3), false, LocalDateTime.now());
    }

    @Test
    void shouldPlaceBid() throws Exception {
        when(biddingService.placeBid(any(PlaceBidRequest.class))).thenReturn(bidResponse);

        mockMvc.perform(post("/bids")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new PlaceBidRequest(10L, BigDecimal.valueOf(120)))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.lotId").value(10))
                .andExpect(jsonPath("$.bidderId").value(20))
                .andExpect(jsonPath("$.amount").value(120));
    }

    @Test
    void shouldRejectInvalidBidPayload() throws Exception {
        mockMvc.perform(post("/bids")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new PlaceBidRequest(null, BigDecimal.ZERO))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.fieldErrors.lotId").exists())
                .andExpect(jsonPath("$.fieldErrors.amount").exists());
    }

    @Test
    void shouldGetBidsByLot() throws Exception {
        when(biddingService.getBidsByLot(anyLong())).thenReturn(List.of(bidResponse));

        mockMvc.perform(get("/bids/lots/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].lotId").value(10));
    }

    @Test
    void shouldGetMyBids() throws Exception {
        when(biddingService.getMyBids()).thenReturn(List.of(bidResponse));

        mockMvc.perform(get("/bids/my"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].bidderId").value(20));
    }

    @Test
    void shouldGetResult() throws Exception {
        when(biddingService.getResult(anyLong())).thenReturn(resultResponse);

        mockMvc.perform(get("/bids/results/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lotId").value(10))
                .andExpect(jsonPath("$.winnerId").value(20))
                .andExpect(jsonPath("$.paid").value(false));
    }
}
