package com.auction.biddingservice.service;

import com.auction.biddingservice.dto.request.FinishLotBiddingRequest;
import com.auction.biddingservice.dto.request.PlaceBidRequest;
import com.auction.biddingservice.dto.response.BidResponse;
import com.auction.biddingservice.dto.response.BidResultResponse;
import com.auction.biddingservice.entity.Bid;
import com.auction.biddingservice.entity.BidResult;
import com.auction.biddingservice.exception.BadRequestException;
import com.auction.biddingservice.exception.ForbiddenException;
import com.auction.biddingservice.exception.NotFoundException;
import com.auction.biddingservice.repository.BidRepository;
import com.auction.biddingservice.repository.BidResultRepository;
import com.auction.biddingservice.security.AuthenticatedUser;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class BiddingService {
    private final BidRepository bids;
    private final BidResultRepository results;
    private final CurrentUserService current;
    private final BiddingMapper mapper;
    private final JdbcTemplate jdbc;

    public BiddingService(BidRepository bids, BidResultRepository results, CurrentUserService current, BiddingMapper mapper, JdbcTemplate jdbc) {
        this.bids = bids;
        this.results = results;
        this.current = current;
        this.mapper = mapper;
        this.jdbc = jdbc;
    }

    @Transactional
    public BidResponse placeBid(PlaceBidRequest request) {
        AuthenticatedUser user = current.getCurrentUser();
        LotSnapshot lot = lockLot(request.lotId());
        validateCanBid(lot, user, request.amount());

        Bid bid = new Bid();
        bid.setLotId(request.lotId());
        bid.setBidderId(user.id());
        bid.setAmount(request.amount());
        Bid saved = bids.save(bid);

        jdbc.update("update auction.lots set current_price = ?, updated_at = current_timestamp where id = ?", request.amount(), request.lotId());
        upsertProvisionalResult(request.lotId(), user.id(), request.amount());
        return mapper.toBidResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<BidResponse> getBidsByLot(Long lotId) {
        ensureLotExists(lotId);
        return bids.findByLotIdOrderByCreatedAtDesc(lotId).stream().map(mapper::toBidResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<BidResponse> getMyBids() {
        Long userId = current.getCurrentUser().id();
        return bids.findByBidderIdOrderByCreatedAtDesc(userId).stream().map(mapper::toBidResponse).toList();
    }

    @Transactional(readOnly = true)
    public BidResultResponse getResult(Long lotId) {
        return mapper.toResultResponse(results.findByLotId(lotId).orElseThrow(() -> new NotFoundException("Bid result not found")));
    }

    @Transactional
    public BidResultResponse finishLotBidding(Long lotId, FinishLotBiddingRequest request) {
        AuthenticatedUser user = current.getCurrentUser();
        admin(user);
        LotSnapshot lot = lockLot(lotId);
        if (!"ACTIVE".equals(lot.status())) throw new BadRequestException("Only active lots can be finished");

        Bid winnerBid = bids.findFirstByLotIdOrderByAmountDescCreatedAtAsc(lotId).orElse(null);
        if (winnerBid == null) {
            markAuctionLotFinished(lotId, null, lot.currentPrice());
            BidResult emptyResult = resultFor(lotId);
            emptyResult.setWinnerId(null);
            emptyResult.setFinalPrice(null);
            emptyResult.setPaymentDeadline(null);
            emptyResult.setPaid(false);
            return mapper.toResultResponse(results.save(emptyResult));
        }

        LocalDateTime paymentDeadline = LocalDateTime.now().plusDays(request.effectivePaymentDeadlineDays());
        markAuctionLotFinished(lotId, winnerBid.getBidderId(), winnerBid.getAmount());
        BidResult result = resultFor(lotId);
        result.setWinnerId(winnerBid.getBidderId());
        result.setFinalPrice(winnerBid.getAmount());
        result.setPaymentDeadline(paymentDeadline);
        result.setPaid(false);
        return mapper.toResultResponse(results.save(result));
    }

    @Transactional
    public BidResultResponse markPaid(Long lotId) {
        AuthenticatedUser user = current.getCurrentUser();
        admin(user);
        BidResult result = results.findByLotId(lotId).orElseThrow(() -> new NotFoundException("Bid result not found"));
        if (result.getWinnerId() == null || result.getFinalPrice() == null) throw new BadRequestException("Lot has no winner");
        result.setPaid(true);
        return mapper.toResultResponse(result);
    }

    private void validateCanBid(LotSnapshot lot, AuthenticatedUser user, BigDecimal amount) {
        if (!"ACTIVE".equals(lot.status())) throw new BadRequestException("Bids are allowed only for active lots");
        if (!lot.endTime().isAfter(LocalDateTime.now())) throw new BadRequestException("Auction time is over");
        if (lot.sellerId().equals(user.id())) throw new ForbiddenException("Seller cannot bid on own lot");
        BigDecimal minimum = lot.currentPrice().add(lot.bidStep());
        if (amount.compareTo(minimum) < 0) throw new BadRequestException("Bid amount must be at least " + minimum);
    }

    private LotSnapshot lockLot(Long lotId) {
        List<LotSnapshot> rows = jdbc.query("""
                select l.id, l.seller_id, s.code, l.current_price, l.bid_step, l.end_time
                from auction.lots l
                join auction.lot_statuses s on s.id = l.status_id
                where l.id = ?
                for update
                """, (rs, rowNum) -> new LotSnapshot(rs.getLong("id"), rs.getLong("seller_id"), rs.getString("code"), rs.getBigDecimal("current_price"), rs.getBigDecimal("bid_step"), rs.getTimestamp("end_time").toLocalDateTime()), lotId);
        if (rows.isEmpty()) throw new NotFoundException("Lot not found");
        return rows.get(0);
    }

    private void ensureLotExists(Long lotId) {
        Integer count = jdbc.queryForObject("select count(*) from auction.lots where id = ?", Integer.class, lotId);
        if (count == null || count == 0) throw new NotFoundException("Lot not found");
    }

    private void upsertProvisionalResult(Long lotId, Long bidderId, BigDecimal amount) {
        jdbc.update("""
                insert into bidding.bid_results (lot_id, winner_id, final_price, paid)
                values (?, ?, ?, false)
                on conflict (lot_id) do update set winner_id = excluded.winner_id, final_price = excluded.final_price, paid = false
                """, lotId, bidderId, amount);
    }

    private BidResult resultFor(Long lotId) {
        return results.findByLotId(lotId).orElseGet(() -> {
            BidResult result = new BidResult();
            result.setLotId(lotId);
            return result;
        });
    }

    private void markAuctionLotFinished(Long lotId, Long winnerId, BigDecimal finalPrice) {
        jdbc.update("""
                update auction.lots
                set status_id = (select id from auction.lot_statuses where code = 'FINISHED'),
                    winner_id = ?, current_price = ?, updated_at = current_timestamp
                where id = ?
                """, winnerId, finalPrice, lotId);
    }

    private void admin(AuthenticatedUser user) {
        if (!current.isAdmin(user)) throw new ForbiddenException("Admin role is required");
    }
}
