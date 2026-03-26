CREATE SCHEMA IF NOT EXISTS bidding;

CREATE TABLE IF NOT EXISTS bidding.bids (
                                            id BIGSERIAL PRIMARY KEY,
                                            lot_id BIGINT NOT NULL,
                                            bidder_id BIGINT NOT NULL,
                                            amount NUMERIC(12, 2) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_bidding_bids_amount
    CHECK (amount > 0)
    );

CREATE TABLE IF NOT EXISTS bidding.bid_results (
                                                   id BIGSERIAL PRIMARY KEY,
                                                   lot_id BIGINT NOT NULL UNIQUE,
                                                   winner_id BIGINT,
                                                   final_price NUMERIC(12, 2),
    payment_deadline TIMESTAMP,
    paid BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_bidding_bid_results_final_price
    CHECK (final_price IS NULL OR final_price > 0),
    CONSTRAINT chk_bidding_bid_results_paid_requires_price
    CHECK (NOT (paid = TRUE AND final_price IS NULL))
    );

CREATE INDEX IF NOT EXISTS idx_bidding_bids_lot_id
    ON bidding.bids(lot_id);

CREATE INDEX IF NOT EXISTS idx_bidding_bids_bidder_id
    ON bidding.bids(bidder_id);

CREATE INDEX IF NOT EXISTS idx_bidding_bids_created_at
    ON bidding.bids(created_at);

CREATE INDEX IF NOT EXISTS idx_bidding_bid_results_winner_id
    ON bidding.bid_results(winner_id);

CREATE INDEX IF NOT EXISTS idx_bidding_bid_results_payment_deadline
    ON bidding.bid_results(payment_deadline);