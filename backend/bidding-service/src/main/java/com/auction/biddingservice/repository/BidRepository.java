package com.auction.biddingservice.repository;

import com.auction.biddingservice.entity.Bid;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BidRepository extends JpaRepository<Bid, Long> {
    List<Bid> findByLotIdOrderByAmountDescCreatedAtAsc(Long lotId);
    List<Bid> findByLotIdOrderByCreatedAtDesc(Long lotId);
    List<Bid> findByBidderIdOrderByCreatedAtDesc(Long bidderId);
    Optional<Bid> findFirstByLotIdOrderByAmountDescCreatedAtAsc(Long lotId);
}
