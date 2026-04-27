package com.auction.auctionservice.repository;

import com.auction.auctionservice.entity.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface LotRepository extends JpaRepository<Lot, Long> {
    List<Lot> findBySellerIdOrderByCreatedAtDesc(Long sellerId);

    @Query("select l from Lot l where (:status is null or l.status.code = :status) and (:categoryId is null or l.category.id = :categoryId) order by l.createdAt desc")
    List<Lot> search(@Param("status") LotStatusCode status, @Param("categoryId") Long categoryId);
}
