package com.auction.auctionservice.repository;

import com.auction.auctionservice.entity.LotStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LotStatusHistoryRepository extends JpaRepository<LotStatusHistory, Long> {
}
