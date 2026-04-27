package com.auction.auctionservice.repository;

import com.auction.auctionservice.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LotStatusRepository extends JpaRepository<LotStatus, Long> {
    Optional<LotStatus> findByCode(LotStatusCode code);
}
