CREATE TABLE bidding.bids (
                                            id BIGSERIAL PRIMARY KEY,
                                            lot_id BIGINT NOT NULL,
                                            bidder_id BIGINT NOT NULL,
                                            amount NUMERIC(12, 2) NOT NULL CHECK (amount > 0),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_bids_lot
    FOREIGN KEY (lot_id) REFERENCES auction.lots(id) ON DELETE CASCADE,
    CONSTRAINT fk_bids_bidder
    FOREIGN KEY (bidder_id) REFERENCES auth.users(id)
    );

CREATE TABLE bidding.bid_results (
                                                   id BIGSERIAL PRIMARY KEY,
                                                   lot_id BIGINT NOT NULL UNIQUE,
                                                   winner_id BIGINT,
                                                   final_price NUMERIC(12, 2),
    payment_deadline TIMESTAMP,
    paid BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_bid_results_lot
    FOREIGN KEY (lot_id) REFERENCES auction.lots(id) ON DELETE CASCADE,
    CONSTRAINT fk_bid_results_winner
    FOREIGN KEY (winner_id) REFERENCES auth.users(id)
    );

CREATE INDEX idx_bids_lot_id ON bidding.bids(lot_id);
CREATE INDEX idx_bids_bidder_id ON bidding.bids(bidder_id);
CREATE INDEX idx_bids_created_at ON bidding.bids(created_at);