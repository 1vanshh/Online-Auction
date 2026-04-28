package com.auction.biddingservice.controller;

import com.auction.biddingservice.dto.request.FinishLotBiddingRequest;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminBiddingController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class AdminBiddingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private BiddingService biddingService;

    private BidResultResponse resultResponse;

    @BeforeEach
    void setUp() {
        objectMapper.registerModule(new JavaTimeModule());
        resultResponse = new BidResultResponse(1L, 10L, 20L, BigDecimal.valueOf(150), LocalDateTime.now().plusDays(3), false, LocalDateTime.now());
    }

    @Test
    void shouldFinishLotBidding() throws Exception {
        when(biddingService.finishLotBidding(anyLong(), any(FinishLotBiddingRequest.class))).thenReturn(resultResponse);

        mockMvc.perform(post("/admin/bids/lots/10/finish")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new FinishLotBiddingRequest(3))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lotId").value(10))
                .andExpect(jsonPath("$.winnerId").value(20))
                .andExpect(jsonPath("$.finalPrice").value(150));
    }

    @Test
    void shouldRejectInvalidPaymentDeadline() throws Exception {
        mockMvc.perform(post("/admin/bids/lots/10/finish")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new FinishLotBiddingRequest(0))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.fieldErrors.paymentDeadlineDays").exists());
    }

    @Test
    void shouldMarkPaid() throws Exception {
        BidResultResponse paid = new BidResultResponse(1L, 10L, 20L, BigDecimal.valueOf(150), LocalDateTime.now().plusDays(3), true, LocalDateTime.now());
        when(biddingService.markPaid(anyLong())).thenReturn(paid);

        mockMvc.perform(post("/admin/bids/lots/10/paid"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paid").value(true));
    }

    @Test
    void shouldReturnAdminHealthCheck() throws Exception {
        mockMvc.perform(post("/admin/bids/health-check"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Bidding admin endpoint is available"));
    }
}
