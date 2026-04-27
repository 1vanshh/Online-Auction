package com.auction.auctionservice.service;

import com.auction.auctionservice.dto.request.*;
import com.auction.auctionservice.dto.response.LotResponse;
import com.auction.auctionservice.entity.*;
import com.auction.auctionservice.exception.*;
import com.auction.auctionservice.mapper.AuctionMapper;
import com.auction.auctionservice.repository.*;
import com.auction.auctionservice.security.AuthenticatedUser;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class LotService {
    private final LotRepository lots;
    private final CategoryRepository cats;
    private final LotStatusRepository statuses;
    private final LotStatusHistoryRepository history;
    private final CurrentUserService current;
    private final AuctionMapper mapper;
    private final JdbcTemplate jdbc;

    public LotService(LotRepository lots, CategoryRepository cats, LotStatusRepository statuses, LotStatusHistoryRepository history, CurrentUserService current, AuctionMapper mapper, JdbcTemplate jdbc) {
        this.lots = lots;
        this.cats = cats;
        this.statuses = statuses;
        this.history = history;
        this.current = current;
        this.mapper = mapper;
        this.jdbc = jdbc;
    }

    @Transactional(readOnly = true)
    public List<LotResponse> search(LotStatusCode status, Long categoryId) {
        return lots.search(status, categoryId).stream().map(mapper::toLotResponse).toList();
    }

    @Transactional(readOnly = true)
    public LotResponse getById(Long id) {
        return mapper.toLotResponse(get(id));
    }

    @Transactional(readOnly = true)
    public List<LotResponse> getMyLots() {
        Long uid = current.getCurrentUser().id();
        return lots.findBySellerIdOrderByCreatedAtDesc(uid).stream().map(mapper::toLotResponse).toList();
    }

    @Transactional
    public LotResponse create(CreateLotRequest r) {
        AuthenticatedUser u = current.getCurrentUser();
        if (!r.endTime().isAfter(LocalDateTime.now())) throw new BadRequestException("End time must be in the future");
        Lot l = new Lot();
        l.setSellerId(u.id());
        l.setCategory(cat(r.categoryId()));
        l.setStatus(status(LotStatusCode.DRAFT));
        l.setTitle(req(r.title(), "Title is required"));
        l.setDescription(trim(r.description()));
        l.setStartPrice(r.startPrice());
        l.setCurrentPrice(r.startPrice());
        l.setBidStep(r.bidStep());
        l.setStartTime(LocalDateTime.now());
        l.setEndTime(r.endTime());
        Lot saved = lots.save(l);
        addHistory(saved, null, saved.getStatus(), u.id(), "Lot created as draft");
        return mapper.toLotResponse(saved);
    }

    @Transactional
    public LotResponse update(Long id, UpdateLotRequest r) {
        AuthenticatedUser u = current.getCurrentUser();
        Lot l = get(id);
        ownerOrAdmin(l, u);
        if (l.getStatus().getCode() != LotStatusCode.DRAFT)
            throw new BadRequestException("Only draft lots can be updated");
        if (r.title() != null) l.setTitle(req(r.title(), "Title is required"));
        if (r.description() != null) l.setDescription(trim(r.description()));
        if (r.categoryId() != null) l.setCategory(cat(r.categoryId()));
        if (r.startPrice() != null) {
            l.setStartPrice(r.startPrice());
            l.setCurrentPrice(r.startPrice());
        }
        if (r.bidStep() != null) l.setBidStep(r.bidStep());
        if (r.endTime() != null) {
            if (!r.endTime().isAfter(LocalDateTime.now()))
                throw new BadRequestException("End time must be in the future");
            l.setEndTime(r.endTime());
        }
        return mapper.toLotResponse(l);
    }

    @Transactional
    public LotResponse activate(Long id) {
        AuthenticatedUser u = current.getCurrentUser();
        Lot l = get(id);
        ownerOrAdmin(l, u);
        if (l.getStatus().getCode() != LotStatusCode.DRAFT)
            throw new BadRequestException("Only draft lots can be activated");
        if (!l.getEndTime().isAfter(LocalDateTime.now()))
            throw new BadRequestException("End time must be in the future");
        change(l, LotStatusCode.ACTIVE, u.id(), "Lot activated");
        return mapper.toLotResponse(l);
    }

    @Transactional
    public LotResponse cancel(Long id, String comment) {
        AuthenticatedUser u = current.getCurrentUser();
        Lot l = get(id);
        ownerOrAdmin(l, u);
        if (l.getStatus().getCode() == LotStatusCode.FINISHED)
            throw new BadRequestException("Finished lot cannot be cancelled");
        change(l, LotStatusCode.CANCELLED, u.id(), trim(comment) == null ? "Lot cancelled" : comment);
        return mapper.toLotResponse(l);
    }

    @Transactional
    public LotResponse finish(Long id) {
        AuthenticatedUser u = current.getCurrentUser();
        admin(u);
        Lot l = get(id);
        if (l.getStatus().getCode() != LotStatusCode.ACTIVE)
            throw new BadRequestException("Only active lots can be finished");
        change(l, LotStatusCode.FINISHED, u.id(), "Lot finished by admin");
        return mapper.toLotResponse(l);
    }

    @Transactional
    public LotResponse setWinner(Long id, SetWinnerRequest r) {
        AuthenticatedUser u = current.getCurrentUser();
        admin(u);
        Lot l = get(id);
        ensureUserExists(r.winnerId());
        l.setWinnerId(r.winnerId());
        if (l.getStatus().getCode() != LotStatusCode.FINISHED)
            change(l, LotStatusCode.FINISHED, u.id(), "Winner assigned by admin");
        return mapper.toLotResponse(l);
    }

    @Transactional
    public void banUnpaidWinner(Long id, UnpaidWinnerBanRequest r) {
        AuthenticatedUser u = current.getCurrentUser();
        admin(u);
        Lot l = get(id);
        if (l.getWinnerId() == null) throw new BadRequestException("Lot has no winner");
        ensureUserExists(l.getWinnerId());
        LocalDateTime until = LocalDateTime.now().plusDays(r.banDays());
        String reason = trim(r.reason());
        if (reason == null) reason = "Winner did not pay for lot #" + l.getId();
        jdbc.update("insert into auth.user_bans (user_id, reason, banned_until, created_by_user_id, active) values (?, ?, ?, ?, true)", l.getWinnerId(), reason, until, u.id());
        jdbc.update("update auth.users set is_banned = true, updated_at = current_timestamp where id = ?", l.getWinnerId());
    }

    private void ensureUserExists(Long id) {
        Integer c = jdbc.queryForObject("select count(*) from auth.users where id = ?", Integer.class, id);
        if (c == null || c == 0) throw new NotFoundException("Winner user not found");
    }

    private Lot get(Long id) {
        return lots.findById(id).orElseThrow(() -> new NotFoundException("Lot not found"));
    }

    private Category cat(Long id) {
        return cats.findById(id).orElseThrow(() -> new NotFoundException("Category not found"));
    }

    private LotStatus status(LotStatusCode c) {
        return statuses.findByCode(c).orElseThrow(() -> new NotFoundException("Lot status not found"));
    }

    private void ownerOrAdmin(Lot l, AuthenticatedUser u) {
        if (!l.getSellerId().equals(u.id()) && !current.isAdmin(u))
            throw new ForbiddenException("Only lot owner or admin can change this lot");
    }

    private void admin(AuthenticatedUser u) {
        if (!current.isAdmin(u)) throw new ForbiddenException("Admin role is required");
    }

    private void change(Lot l, LotStatusCode nc, Long actor, String comment) {
        LotStatus old = l.getStatus(), next = status(nc);
        l.setStatus(next);
        addHistory(l, old, next, actor, comment);
    }

    private void addHistory(Lot l, LotStatus old, LotStatus next, Long actor, String comment) {
        LotStatusHistory h = new LotStatusHistory();
        h.setLot(l);
        h.setOldStatus(old);
        h.setNewStatus(next);
        h.setChangedByUserId(actor);
        h.setComment(comment);
        history.save(h);
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
