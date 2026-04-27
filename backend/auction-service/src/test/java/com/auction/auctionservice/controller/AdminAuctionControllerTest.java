package com.auction.auctionservice.controller;

import com.auction.auctionservice.dto.request.SetWinnerRequest;
import com.auction.auctionservice.dto.request.UnpaidWinnerBanRequest;
import com.auction.auctionservice.dto.response.LotResponse;
import com.auction.auctionservice.entity.LotStatusCode;
import com.auction.auctionservice.exception.GlobalExceptionHandler;
import com.auction.auctionservice.service.LotService;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminAuctionController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class AdminAuctionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private LotService lotService;

    private LotResponse lotResponse;

    @BeforeEach
    void setUp() {
        lotResponse = new LotResponse(
                1L,
                10L,
                2L,
                "Books",
                LotStatusCode.FINISHED,
                "Clean Code",
                "Book",
                BigDecimal.valueOf(100),
                BigDecimal.valueOf(100),
                BigDecimal.TEN,
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(1),
                20L,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }

    @Test
    void shouldFinishLot() throws Exception {
        when(lotService.finish(anyLong())).thenReturn(lotResponse);

        mockMvc.perform(post("/admin/lots/1/finish"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FINISHED"));
    }

    @Test
    void shouldSetWinner() throws Exception {
        when(lotService.setWinner(anyLong(), any(SetWinnerRequest.class))).thenReturn(lotResponse);

        mockMvc.perform(post("/admin/lots/1/winner")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SetWinnerRequest(20L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.winnerId").value(20));
    }

    @Test
    void shouldRejectInvalidWinnerPayload() throws Exception {
        mockMvc.perform(post("/admin/lots/1/winner")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SetWinnerRequest(null))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.fieldErrors.winnerId").exists());
    }

    @Test
    void shouldBanUnpaidWinner() throws Exception {
        mockMvc.perform(post("/admin/lots/1/unpaid-ban")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UnpaidWinnerBanRequest(30, "Not paid"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Winner banned for non-payment"));

        verify(lotService).banUnpaidWinner(anyLong(), any(UnpaidWinnerBanRequest.class));
    }

    @Test
    void shouldRejectInvalidBanDays() throws Exception {
        mockMvc.perform(post("/admin/lots/1/unpaid-ban")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UnpaidWinnerBanRequest(0, "Not paid"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.banDays").exists());
    }
}
