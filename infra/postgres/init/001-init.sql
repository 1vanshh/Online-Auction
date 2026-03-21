-- =========================================================
-- Online Auction MVP
-- PostgreSQL init schema
-- =========================================================

-- =====================
-- Schemas
-- =====================
CREATE SCHEMA IF NOT EXISTS auth;
CREATE SCHEMA IF NOT EXISTS auction;
CREATE SCHEMA IF NOT EXISTS bidding;

-- =====================
-- AUTH SCHEMA
-- =====================

CREATE TABLE IF NOT EXISTS auth.roles (
                                          id BIGSERIAL PRIMARY KEY,
                                          name VARCHAR(50) NOT NULL UNIQUE
    );

CREATE TABLE IF NOT EXISTS auth.users (
                                          id BIGSERIAL PRIMARY KEY,
                                          first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100),
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    phone VARCHAR(30),
    birth_date DATE,
    country VARCHAR(100),
    city VARCHAR(100),
    address_line VARCHAR(255),
    postal_code VARCHAR(20),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    is_banned BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
    );

CREATE TABLE IF NOT EXISTS auth.user_roles (
                                               user_id BIGINT NOT NULL,
                                               role_id BIGINT NOT NULL,
                                               PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_roles_user
    FOREIGN KEY (user_id) REFERENCES auth.users(id) ON DELETE CASCADE,
    CONSTRAINT fk_user_roles_role
    FOREIGN KEY (role_id) REFERENCES auth.roles(id) ON DELETE CASCADE
    );

CREATE TABLE IF NOT EXISTS auth.user_bans (
                                              id BIGSERIAL PRIMARY KEY,
                                              user_id BIGINT NOT NULL,
                                              reason VARCHAR(255) NOT NULL,
    banned_from TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    banned_until TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_user_bans_user
    FOREIGN KEY (user_id) REFERENCES auth.users(id) ON DELETE CASCADE
    );

CREATE TABLE IF NOT EXISTS auth.refresh_tokens (
                                                   id BIGSERIAL PRIMARY KEY,
                                                   user_id BIGINT NOT NULL,
                                                   token VARCHAR(512) NOT NULL UNIQUE,
    expires_at TIMESTAMP NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_refresh_tokens_user
    FOREIGN KEY (user_id) REFERENCES auth.users(id) ON DELETE CASCADE
    );

CREATE INDEX IF NOT EXISTS idx_auth_users_email ON auth.users(email);
CREATE INDEX IF NOT EXISTS idx_auth_user_bans_user_id ON auth.user_bans(user_id);
CREATE INDEX IF NOT EXISTS idx_auth_refresh_tokens_user_id ON auth.refresh_tokens(user_id);

-- =====================
-- AUCTION SCHEMA
-- =====================

CREATE TABLE IF NOT EXISTS auction.categories (
                                                  id BIGSERIAL PRIMARY KEY,
                                                  name VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(255)
    );

CREATE TABLE IF NOT EXISTS auction.lot_statuses (
                                                    id BIGSERIAL PRIMARY KEY,
                                                    code VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255)
    );

CREATE TABLE IF NOT EXISTS auction.lots (
                                            id BIGSERIAL PRIMARY KEY,
                                            seller_id BIGINT NOT NULL,
                                            category_id BIGINT NOT NULL,
                                            status_id BIGINT NOT NULL,
                                            title VARCHAR(255) NOT NULL,
    description TEXT,
    start_price NUMERIC(12, 2) NOT NULL CHECK (start_price > 0),
    current_price NUMERIC(12, 2) NOT NULL CHECK (current_price >= 0),
    bid_step NUMERIC(12, 2) NOT NULL CHECK (bid_step > 0),
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP NOT NULL,
    winner_id BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_lots_time CHECK (end_time > start_time),
    CONSTRAINT fk_lots_category
    FOREIGN KEY (category_id) REFERENCES auction.categories(id),
    CONSTRAINT fk_lots_status
    FOREIGN KEY (status_id) REFERENCES auction.lot_statuses(id),
    CONSTRAINT fk_lots_seller
    FOREIGN KEY (seller_id) REFERENCES auth.users(id),
    CONSTRAINT fk_lots_winner
    FOREIGN KEY (winner_id) REFERENCES auth.users(id)
    );

CREATE TABLE IF NOT EXISTS auction.lot_images (
                                                  id BIGSERIAL PRIMARY KEY,
                                                  lot_id BIGINT NOT NULL,
                                                  image_url VARCHAR(500) NOT NULL,
    is_main BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_lot_images_lot
    FOREIGN KEY (lot_id) REFERENCES auction.lots(id) ON DELETE CASCADE
    );

CREATE TABLE IF NOT EXISTS auction.lot_status_history (
                                                          id BIGSERIAL PRIMARY KEY,
                                                          lot_id BIGINT NOT NULL,
                                                          old_status_id BIGINT,
                                                          new_status_id BIGINT NOT NULL,
                                                          changed_by_user_id BIGINT,
                                                          changed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                                          comment VARCHAR(255),
    CONSTRAINT fk_lot_status_history_lot
    FOREIGN KEY (lot_id) REFERENCES auction.lots(id) ON DELETE CASCADE,
    CONSTRAINT fk_lot_status_history_old_status
    FOREIGN KEY (old_status_id) REFERENCES auction.lot_statuses(id),
    CONSTRAINT fk_lot_status_history_new_status
    FOREIGN KEY (new_status_id) REFERENCES auction.lot_statuses(id),
    CONSTRAINT fk_lot_status_history_changed_by
    FOREIGN KEY (changed_by_user_id) REFERENCES auth.users(id)
    );

CREATE INDEX IF NOT EXISTS idx_auction_lots_seller_id ON auction.lots(seller_id);
CREATE INDEX IF NOT EXISTS idx_auction_lots_category_id ON auction.lots(category_id);
CREATE INDEX IF NOT EXISTS idx_auction_lots_status_id ON auction.lots(status_id);
CREATE INDEX IF NOT EXISTS idx_auction_lots_end_time ON auction.lots(end_time);

-- =====================
-- BIDDING SCHEMA
-- =====================

CREATE TABLE IF NOT EXISTS bidding.bids (
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

CREATE TABLE IF NOT EXISTS bidding.bid_results (
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

CREATE INDEX IF NOT EXISTS idx_bids_lot_id ON bidding.bids(lot_id);
CREATE INDEX IF NOT EXISTS idx_bids_bidder_id ON bidding.bids(bidder_id);
CREATE INDEX IF NOT EXISTS idx_bids_created_at ON bidding.bids(created_at);

-- =====================
-- AUDIT LOG (optional, but useful)
-- =====================

CREATE TABLE IF NOT EXISTS auth.audit_log (
                                              id BIGSERIAL PRIMARY KEY,
                                              user_id BIGINT,
                                              action VARCHAR(100) NOT NULL,
    entity_type VARCHAR(100) NOT NULL,
    entity_id BIGINT,
    details TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_audit_log_user
    FOREIGN KEY (user_id) REFERENCES auth.users(id) ON DELETE SET NULL
    );

CREATE INDEX IF NOT EXISTS idx_audit_log_user_id ON auth.audit_log(user_id);
CREATE INDEX IF NOT EXISTS idx_audit_log_created_at ON auth.audit_log(created_at);

-- =====================
-- Seed data
-- =====================

INSERT INTO auth.roles (name)
VALUES ('USER'), ('ADMIN')
    ON CONFLICT (name) DO NOTHING;

INSERT INTO auction.lot_statuses (code, description)
VALUES
    ('DRAFT', 'Draft lot'),
    ('ACTIVE', 'Active auction'),
    ('FINISHED', 'Finished auction'),
    ('CANCELLED', 'Cancelled auction')
    ON CONFLICT (code) DO NOTHING;

INSERT INTO auction.categories (name, description)
VALUES
    ('Electronics', 'Phones, laptops, gadgets'),
    ('Books', 'Books and printed materials'),
    ('Clothing', 'Clothes and accessories'),
    ('Home', 'Home goods'),
    ('Other', 'Other goods')
    ON CONFLICT (name) DO NOTHING;