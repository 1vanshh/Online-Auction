package com.auction.auctionservice.service;

import com.auction.auctionservice.dto.request.*;
import com.auction.auctionservice.dto.response.LotResponse;
import com.auction.auctionservice.entity.*;
import com.auction.auctionservice.exception.BadRequestException;
import com.auction.auctionservice.exception.ForbiddenException;
import com.auction.auctionservice.exception.NotFoundException;
import com.auction.auctionservice.mapper.AuctionMapper;
import com.auction.auctionservice.repository.*;
import com.auction.auctionservice.security.AuthenticatedUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LotServiceTest {

    @Mock
    private LotRepository lots;
    @Mock
    private CategoryRepository cats;
    @Mock
    private LotStatusRepository statuses;
    @Mock
    private LotStatusHistoryRepository history;
    @Mock
    private CurrentUserService current;
    @Mock
    private AuctionMapper mapper;
    @Mock
    private JdbcTemplate jdbc;

    @InjectMocks
    private LotService service;

    private AuthenticatedUser user;
    private AuthenticatedUser admin;
    private Category category;
    private LotStatus draft;
    private LotStatus active;
    private LotStatus finished;
    private Lot lot;
    private LotResponse response;

    @BeforeEach
    void setUp() {
        user = new AuthenticatedUser(10L, "seller@example.com", Role.USER);
        admin = new AuthenticatedUser(99L, "admin@example.com", Role.ADMIN);

        category = new Category();
        category.setId(1L);
        category.setName("Electronics");

        draft = status(1L, LotStatusCode.DRAFT);
        active = status(2L, LotStatusCode.ACTIVE);
        finished = status(3L, LotStatusCode.FINISHED);

        lot = new Lot();
        lot.setId(5L);
        lot.setSellerId(10L);
        lot.setCategory(category);
        lot.setStatus(draft);
        lot.setTitle("iPhone");
        lot.setDescription("Phone");
        lot.setStartPrice(BigDecimal.valueOf(100));
        lot.setCurrentPrice(BigDecimal.valueOf(100));
        lot.setBidStep(BigDecimal.TEN);
        lot.setStartTime(LocalDateTime.now());
        lot.setEndTime(LocalDateTime.now().plusDays(1));

        response = new LotResponse(5L, 10L, 1L, "Electronics", LotStatusCode.DRAFT, "iPhone", "Phone",
                BigDecimal.valueOf(100), BigDecimal.valueOf(100), BigDecimal.TEN,
                lot.getStartTime(), lot.getEndTime(), null, null, null);
    }

    @Test
    void shouldCreateDraftLot() {
        CreateLotRequest request = new CreateLotRequest(" iPhone ", " Phone ", 1L, BigDecimal.valueOf(100), BigDecimal.TEN, LocalDateTime.now().plusDays(2));
        when(current.getCurrentUser()).thenReturn(user);
        when(cats.findById(1L)).thenReturn(Optional.of(category));
        when(statuses.findByCode(LotStatusCode.DRAFT)).thenReturn(Optional.of(draft));
        when(lots.save(any(Lot.class))).thenAnswer(invocation -> {
            Lot saved = invocation.getArgument(0);
            saved.setId(5L);
            return saved;
        });
        when(mapper.toLotResponse(any(Lot.class))).thenReturn(response);

        LotResponse result = service.create(request);

        assertSame(response, result);
        verify(lots).save(argThat(l -> l.getSellerId().equals(10L)
                && l.getTitle().equals("iPhone")
                && l.getStatus().getCode() == LotStatusCode.DRAFT
                && l.getCurrentPrice().compareTo(BigDecimal.valueOf(100)) == 0));
        verify(history).save(any(LotStatusHistory.class));
    }

    @Test
    void shouldRejectCreateWithPastEndTime() {
        CreateLotRequest request = new CreateLotRequest("Lot", null, 1L, BigDecimal.ONE, BigDecimal.ONE, LocalDateTime.now().minusMinutes(1));
        when(current.getCurrentUser()).thenReturn(user);

        BadRequestException ex = assertThrows(BadRequestException.class, () -> service.create(request));

        assertEquals("End time must be in the future", ex.getMessage());
        verify(lots, never()).save(any());
    }

    @Test
    void shouldActivateDraftLotByOwner() {
        when(current.getCurrentUser()).thenReturn(user);
        when(lots.findById(5L)).thenReturn(Optional.of(lot));
        when(statuses.findByCode(LotStatusCode.ACTIVE)).thenReturn(Optional.of(active));
        when(mapper.toLotResponse(lot)).thenReturn(response);

        service.activate(5L);

        assertSame(active, lot.getStatus());
        verify(history).save(argThat(h -> h.getOldStatus() == draft && h.getNewStatus() == active));
    }

    @Test
    void shouldRejectUpdateWhenNotOwnerOrAdmin() {
        lot.setSellerId(11L);
        when(current.getCurrentUser()).thenReturn(user);
        when(current.isAdmin(user)).thenReturn(false);
        when(lots.findById(5L)).thenReturn(Optional.of(lot));

        ForbiddenException ex = assertThrows(ForbiddenException.class,
                () -> service.update(5L, new UpdateLotRequest("New title", null, null, null, null, null)));

        assertEquals("Only lot owner or admin can change this lot", ex.getMessage());
    }

    @Test
    void shouldRejectUpdateOfActiveLot() {
        lot.setStatus(active);
        when(current.getCurrentUser()).thenReturn(user);
        when(lots.findById(5L)).thenReturn(Optional.of(lot));

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> service.update(5L, new UpdateLotRequest("New title", null, null, null, null, null)));

        assertEquals("Only draft lots can be updated", ex.getMessage());
    }

    @Test
    void shouldFinishActiveLotByAdmin() {
        lot.setStatus(active);
        when(current.getCurrentUser()).thenReturn(admin);
        when(current.isAdmin(admin)).thenReturn(true);
        when(lots.findById(5L)).thenReturn(Optional.of(lot));
        when(statuses.findByCode(LotStatusCode.FINISHED)).thenReturn(Optional.of(finished));
        when(mapper.toLotResponse(lot)).thenReturn(response);

        service.finish(5L);

        assertSame(finished, lot.getStatus());
        verify(history).save(any(LotStatusHistory.class));
    }

    @Test
    void shouldRejectFinishByUser() {
        when(current.getCurrentUser()).thenReturn(user);
        when(current.isAdmin(user)).thenReturn(false);

        ForbiddenException ex = assertThrows(ForbiddenException.class, () -> service.finish(5L));

        assertEquals("Admin role is required", ex.getMessage());
    }

    @Test
    void shouldSetWinnerAndFinishLot() {
        when(current.getCurrentUser()).thenReturn(admin);
        when(current.isAdmin(admin)).thenReturn(true);
        when(lots.findById(5L)).thenReturn(Optional.of(lot));
        when(jdbc.queryForObject("select count(*) from auth.users where id = ?", Integer.class, 20L)).thenReturn(1);
        when(statuses.findByCode(LotStatusCode.FINISHED)).thenReturn(Optional.of(finished));
        when(mapper.toLotResponse(lot)).thenReturn(response);

        service.setWinner(5L, new SetWinnerRequest(20L));

        assertEquals(20L, lot.getWinnerId());
        assertSame(finished, lot.getStatus());
    }

    @Test
    void shouldBanUnpaidWinnerAndDeactivateUser() {
        lot.setWinnerId(20L);
        when(current.getCurrentUser()).thenReturn(admin);
        when(current.isAdmin(admin)).thenReturn(true);
        when(lots.findById(5L)).thenReturn(Optional.of(lot));
        when(jdbc.queryForObject("select count(*) from auth.users where id = ?", Integer.class, 20L)).thenReturn(1);

        service.banUnpaidWinner(5L, new UnpaidWinnerBanRequest(30, "Not paid"));

        verify(jdbc).update(startsWith("insert into auth.user_bans"), eq(20L), eq("Not paid"), any(LocalDateTime.class), eq(99L));
        verify(jdbc).update("update auth.users set is_banned = true, is_active = false, updated_at = current_timestamp where id = ?", 20L);
    }

    @Test
    void shouldThrowWhenLotNotFound() {
        when(lots.findById(404L)).thenReturn(Optional.empty());

        NotFoundException ex = assertThrows(NotFoundException.class, () -> service.getById(404L));

        assertEquals("Lot not found", ex.getMessage());
    }

    @Test
    void shouldSearchLots() {
        when(lots.search(LotStatusCode.ACTIVE, 1L)).thenReturn(List.of(lot));
        when(mapper.toLotResponse(lot)).thenReturn(response);

        List<LotResponse> result = service.search(LotStatusCode.ACTIVE, 1L);

        assertEquals(1, result.size());
        assertSame(response, result.getFirst());
    }

    private LotStatus status(Long id, LotStatusCode code) {
        LotStatus status = new LotStatus();
        status.setId(id);
        status.setCode(code);
        status.setDescription(code.name());
        return status;
    }
}
