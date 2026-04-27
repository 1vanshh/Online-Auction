package com.auction.auctionservice.entity;

import jakarta.persistence.*;
import lombok.*;

@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "lot_statuses", schema = "auction")
public class LotStatus {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true, length = 50)
    private LotStatusCode code;
    @Column(nullable = false, length = 255)
    private String description;
}
