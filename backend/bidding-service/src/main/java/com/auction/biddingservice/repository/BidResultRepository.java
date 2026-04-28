package com.auction.biddingservice.repository;

import com.auction.biddingservice.entity.BidResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BidResultRepository extends JpaRepository<BidResult, Long> {
    Optional<BidResult> findByLotId(Long lotId);
}
