package com.auction.auctionservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "lot_status_history", schema = "auction")
public class LotStatusHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lot_id", nullable = false)
    private Lot lot;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "old_status_id")
    private LotStatus oldStatus;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "new_status_id", nullable = false)
    private LotStatus newStatus;
    @Column(name = "changed_by_user_id")
    private Long changedByUserId;
    @Column(name = "changed_at", nullable = false, updatable = false)
    private LocalDateTime changedAt;
    @Column(length = 255)
    private String comment;

    @PrePersist
    public void onCreate() {
        changedAt = LocalDateTime.now();
    }
}
