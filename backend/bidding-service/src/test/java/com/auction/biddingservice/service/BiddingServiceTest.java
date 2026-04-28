package com.auction.biddingservice.service;

import com.auction.biddingservice.dto.request.FinishLotBiddingRequest;
import com.auction.biddingservice.dto.request.PlaceBidRequest;
import com.auction.biddingservice.dto.response.BidResponse;
import com.auction.biddingservice.dto.response.BidResultResponse;
import com.auction.biddingservice.entity.Bid;
import com.auction.biddingservice.entity.BidResult;
import com.auction.biddingservice.entity.Role;
import com.auction.biddingservice.exception.BadRequestException;
import com.auction.biddingservice.exception.ForbiddenException;
import com.auction.biddingservice.exception.NotFoundException;
import com.auction.biddingservice.repository.BidRepository;
import com.auction.biddingservice.repository.BidResultRepository;
import com.auction.biddingservice.security.AuthenticatedUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import static org.mockito.Mockito.lenient;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BiddingServiceTest {
    @Mock
    private BidRepository bids;
    @Mock
    private BidResultRepository results;
    @Mock
    private CurrentUserService current;
    @Mock
    private JdbcTemplate jdbc;

    private BiddingService service;
    private final BiddingMapper mapper = new BiddingMapper();
    private final AuthenticatedUser bidder = new AuthenticatedUser(20L, "bidder@test.com", Role.USER);
    private final AuthenticatedUser admin = new AuthenticatedUser(1L, "admin@test.com", Role.ADMIN);

    @BeforeEach
    void setUp() {
        service = new BiddingService(bids, results, current, mapper, jdbc);
    }

    @Test
    void shouldPlaceBidAndUpdateAuctionLot() {
        when(current.getCurrentUser()).thenReturn(bidder);
        mockLockedLot(new LotSnapshot(10L, 99L, "ACTIVE", BigDecimal.valueOf(100), BigDecimal.TEN, LocalDateTime.now().plusDays(1)));
        when(bids.save(any(Bid.class))).thenAnswer(invocation -> {
            Bid bid = invocation.getArgument(0);
            bid.setId(1L);
            bid.setCreatedAt(LocalDateTime.now());
            return bid;
        });
        lenient().when(jdbc.update(anyString(), any(), any())).thenReturn(1);
        lenient().when(jdbc.update(anyString(), any(), any(), any())).thenReturn(1);

        BidResponse response = service.placeBid(new PlaceBidRequest(10L, BigDecimal.valueOf(120)));

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.lotId()).isEqualTo(10L);
        assertThat(response.bidderId()).isEqualTo(20L);
        assertThat(response.amount()).isEqualByComparingTo("120");
        verify(jdbc).update(eq("update auction.lots set current_price = ?, updated_at = current_timestamp where id = ?"), eq(BigDecimal.valueOf(120)), eq(10L));
        verify(jdbc).update(ArgumentMatchers.contains("insert into bidding.bid_results"), eq(10L), eq(20L), eq(BigDecimal.valueOf(120)));
    }

    @Test
    void shouldRejectBidForInactiveLot() {
        when(current.getCurrentUser()).thenReturn(bidder);
        mockLockedLot(new LotSnapshot(10L, 99L, "DRAFT", BigDecimal.valueOf(100), BigDecimal.TEN, LocalDateTime.now().plusDays(1)));

        assertThatThrownBy(() -> service.placeBid(new PlaceBidRequest(10L, BigDecimal.valueOf(120))))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Bids are allowed only for active lots");

        verify(bids, never()).save(any(Bid.class));
    }

    @Test
    void shouldRejectBidAfterAuctionTimeIsOver() {
        when(current.getCurrentUser()).thenReturn(bidder);
        mockLockedLot(new LotSnapshot(10L, 99L, "ACTIVE", BigDecimal.valueOf(100), BigDecimal.TEN, LocalDateTime.now().minusMinutes(1)));

        assertThatThrownBy(() -> service.placeBid(new PlaceBidRequest(10L, BigDecimal.valueOf(120))))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Auction time is over");
    }

    @Test
    void shouldRejectSellerBidOnOwnLot() {
        when(current.getCurrentUser()).thenReturn(new AuthenticatedUser(99L, "seller@test.com", Role.USER));
        mockLockedLot(new LotSnapshot(10L, 99L, "ACTIVE", BigDecimal.valueOf(100), BigDecimal.TEN, LocalDateTime.now().plusDays(1)));

        assertThatThrownBy(() -> service.placeBid(new PlaceBidRequest(10L, BigDecimal.valueOf(120))))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("Seller cannot bid on own lot");
    }

    @Test
    void shouldRejectTooSmallBid() {
        when(current.getCurrentUser()).thenReturn(bidder);
        mockLockedLot(new LotSnapshot(10L, 99L, "ACTIVE", BigDecimal.valueOf(100), BigDecimal.TEN, LocalDateTime.now().plusDays(1)));

        assertThatThrownBy(() -> service.placeBid(new PlaceBidRequest(10L, BigDecimal.valueOf(109))))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Bid amount must be at least 110");
    }

    @Test
    void shouldGetBidsByLot() {
        when(jdbc.queryForObject(anyString(), eq(Integer.class), eq(10L))).thenReturn(1);
        Bid bid = bid(1L, 10L, 20L, "120");
        when(bids.findByLotIdOrderByCreatedAtDesc(anyLong())).thenReturn(List.of(bid));

        List<BidResponse> responses = service.getBidsByLot(10L);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).amount()).isEqualByComparingTo("120");
    }

    @Test
    void shouldThrowWhenLotDoesNotExistWhileGettingBids() {
        when(jdbc.queryForObject(anyString(), eq(Integer.class), eq(10L))).thenReturn(0);

        assertThatThrownBy(() -> service.getBidsByLot(10L))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Lot not found");
    }

    @Test
    void shouldGetCurrentUserBids() {
        when(current.getCurrentUser()).thenReturn(bidder);
        when(bids.findByBidderIdOrderByCreatedAtDesc(20L)).thenReturn(List.of(bid(1L, 10L, 20L, "120")));

        List<BidResponse> responses = service.getMyBids();

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).bidderId()).isEqualTo(20L);
    }

    @Test
    void shouldGetResult() {
        BidResult result = result(1L, 10L, 20L, "150", false);
        when(results.findByLotId(10L)).thenReturn(Optional.of(result));

        BidResultResponse response = service.getResult(10L);

        assertThat(response.lotId()).isEqualTo(10L);
        assertThat(response.winnerId()).isEqualTo(20L);
        assertThat(response.finalPrice()).isEqualByComparingTo("150");
    }

    @Test
    void shouldFinishLotBiddingWithWinner() {
        when(current.getCurrentUser()).thenReturn(admin);
        when(current.isAdmin(admin)).thenReturn(true);
        mockLockedLot(new LotSnapshot(10L, 99L, "ACTIVE", BigDecimal.valueOf(100), BigDecimal.TEN, LocalDateTime.now().plusDays(1)));
        Bid winnerBid = bid(1L, 10L, 20L, "150");
        when(bids.findFirstByLotIdOrderByAmountDescCreatedAtAsc(10L)).thenReturn(Optional.of(winnerBid));
        when(results.findByLotId(10L)).thenReturn(Optional.empty());
        when(results.save(any(BidResult.class))).thenAnswer(invocation -> {
            BidResult result = invocation.getArgument(0);
            result.setId(1L);
            result.setCreatedAt(LocalDateTime.now());
            return result;
        });

        BidResultResponse response = service.finishLotBidding(10L, new FinishLotBiddingRequest(5));

        assertThat(response.lotId()).isEqualTo(10L);
        assertThat(response.winnerId()).isEqualTo(20L);
        assertThat(response.finalPrice()).isEqualByComparingTo("150");
        assertThat(response.paymentDeadline()).isAfter(LocalDateTime.now().plusDays(4));
        verify(jdbc).update(ArgumentMatchers.contains("update auction.lots"), eq(20L), eq(BigDecimal.valueOf(150)), eq(10L));
    }

    @Test
    void shouldFinishLotBiddingWithoutWinner() {
        when(current.getCurrentUser()).thenReturn(admin);
        when(current.isAdmin(admin)).thenReturn(true);
        mockLockedLot(new LotSnapshot(10L, 99L, "ACTIVE", BigDecimal.valueOf(100), BigDecimal.TEN, LocalDateTime.now().plusDays(1)));
        when(bids.findFirstByLotIdOrderByAmountDescCreatedAtAsc(10L)).thenReturn(Optional.empty());
        when(results.findByLotId(10L)).thenReturn(Optional.empty());
        when(results.save(any(BidResult.class))).thenAnswer(invocation -> {
            BidResult result = invocation.getArgument(0);
            result.setId(1L);
            result.setCreatedAt(LocalDateTime.now());
            return result;
        });

        BidResultResponse response = service.finishLotBidding(10L, new FinishLotBiddingRequest(null));

        assertThat(response.lotId()).isEqualTo(10L);
        assertThat(response.winnerId()).isNull();
        assertThat(response.finalPrice()).isNull();
        assertThat(response.paymentDeadline()).isNull();
        verify(jdbc).update(ArgumentMatchers.contains("update auction.lots"), eq(null), eq(BigDecimal.valueOf(100)), eq(10L));
    }

    @Test
    void shouldRejectFinishLotBiddingForNonAdmin() {
        when(current.getCurrentUser()).thenReturn(bidder);
        when(current.isAdmin(bidder)).thenReturn(false);

        assertThatThrownBy(() -> service.finishLotBidding(10L, new FinishLotBiddingRequest(3)))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("Admin role is required");
    }

    @Test
    void shouldRejectFinishForNonActiveLot() {
        when(current.getCurrentUser()).thenReturn(admin);
        when(current.isAdmin(admin)).thenReturn(true);
        mockLockedLot(new LotSnapshot(10L, 99L, "FINISHED", BigDecimal.valueOf(100), BigDecimal.TEN, LocalDateTime.now().plusDays(1)));

        assertThatThrownBy(() -> service.finishLotBidding(10L, new FinishLotBiddingRequest(3)))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Only active lots can be finished");
    }

    @Test
    void shouldMarkPaid() {
        when(current.getCurrentUser()).thenReturn(admin);
        when(current.isAdmin(admin)).thenReturn(true);
        when(results.findByLotId(10L)).thenReturn(Optional.of(result(1L, 10L, 20L, "150", false)));

        BidResultResponse response = service.markPaid(10L);

        assertThat(response.paid()).isTrue();
    }

    @Test
    void shouldRejectMarkPaidWhenLotHasNoWinner() {
        when(current.getCurrentUser()).thenReturn(admin);
        when(current.isAdmin(admin)).thenReturn(true);
        when(results.findByLotId(10L)).thenReturn(Optional.of(result(1L, 10L, null, null, false)));

        assertThatThrownBy(() -> service.markPaid(10L))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Lot has no winner");
    }

    private void mockLockedLot(LotSnapshot lot) {
        when(jdbc.query(anyString(), ArgumentMatchers.<RowMapper<LotSnapshot>>any(), anyLong())).thenReturn(List.of(lot));
    }

    private Bid bid(Long id, Long lotId, Long bidderId, String amount) {
        Bid bid = new Bid();
        bid.setId(id);
        bid.setLotId(lotId);
        bid.setBidderId(bidderId);
        bid.setAmount(amount == null ? null : new BigDecimal(amount));
        bid.setCreatedAt(LocalDateTime.now());
        return bid;
    }

    private BidResult result(Long id, Long lotId, Long winnerId, String finalPrice, boolean paid) {
        BidResult result = new BidResult();
        result.setId(id);
        result.setLotId(lotId);
        result.setWinnerId(winnerId);
        result.setFinalPrice(finalPrice == null ? null : new BigDecimal(finalPrice));
        result.setPaymentDeadline(winnerId == null ? null : LocalDateTime.now().plusDays(3));
        result.setPaid(paid);
        result.setCreatedAt(LocalDateTime.now());
        return result;
    }
}
