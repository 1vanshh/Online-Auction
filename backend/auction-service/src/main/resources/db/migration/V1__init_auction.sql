CREATE SCHEMA IF NOT EXISTS auction;

CREATE TABLE IF NOT EXISTS auction.categories (
                                                  id BIGSERIAL PRIMARY KEY,
                                                  name VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_auction_categories_name
    CHECK (char_length(trim(name)) BETWEEN 2 AND 100)
    );

CREATE TABLE IF NOT EXISTS auction.lot_statuses (
                                                    id BIGSERIAL PRIMARY KEY,
                                                    code VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255) NOT NULL,
    CONSTRAINT chk_auction_lot_statuses_code
    CHECK (code IN ('DRAFT', 'ACTIVE', 'FINISHED', 'CANCELLED'))
    );

CREATE TABLE IF NOT EXISTS auction.lots (
                                            id BIGSERIAL PRIMARY KEY,
                                            seller_id BIGINT NOT NULL,
                                            category_id BIGINT NOT NULL,
                                            status_id BIGINT NOT NULL,
                                            title VARCHAR(255) NOT NULL,
    description TEXT,
    start_price NUMERIC(12, 2) NOT NULL,
    current_price NUMERIC(12, 2) NOT NULL,
    bid_step NUMERIC(12, 2) NOT NULL,
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP NOT NULL,
    winner_id BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_auction_lots_category
    FOREIGN KEY (category_id) REFERENCES auction.categories(id),
    CONSTRAINT fk_auction_lots_status
    FOREIGN KEY (status_id) REFERENCES auction.lot_statuses(id),
    CONSTRAINT chk_auction_lots_title
    CHECK (char_length(trim(title)) BETWEEN 3 AND 255),
    CONSTRAINT chk_auction_lots_start_price
    CHECK (start_price > 0),
    CONSTRAINT chk_auction_lots_current_price
    CHECK (current_price >= 0),
    CONSTRAINT chk_auction_lots_bid_step
    CHECK (bid_step > 0),
    CONSTRAINT chk_auction_lots_current_vs_start
    CHECK (current_price >= start_price),
    CONSTRAINT chk_auction_lots_time
    CHECK (end_time > start_time)
    );

CREATE TABLE IF NOT EXISTS auction.lot_images (
                                                  id BIGSERIAL PRIMARY KEY,
                                                  lot_id BIGINT NOT NULL,
                                                  image_url VARCHAR(500) NOT NULL,
    is_main BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_auction_lot_images_lot
    FOREIGN KEY (lot_id) REFERENCES auction.lots(id) ON DELETE CASCADE,
    CONSTRAINT chk_auction_lot_images_url
    CHECK (char_length(trim(image_url)) BETWEEN 5 AND 500)
    );

CREATE TABLE IF NOT EXISTS auction.lot_status_history (
                                                          id BIGSERIAL PRIMARY KEY,
                                                          lot_id BIGINT NOT NULL,
                                                          old_status_id BIGINT,
                                                          new_status_id BIGINT NOT NULL,
                                                          changed_by_user_id BIGINT,
                                                          changed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                                          comment VARCHAR(255),
    CONSTRAINT fk_auction_lot_status_history_lot
    FOREIGN KEY (lot_id) REFERENCES auction.lots(id) ON DELETE CASCADE,
    CONSTRAINT fk_auction_lot_status_history_old_status
    FOREIGN KEY (old_status_id) REFERENCES auction.lot_statuses(id),
    CONSTRAINT fk_auction_lot_status_history_new_status
    FOREIGN KEY (new_status_id) REFERENCES auction.lot_statuses(id)
    );

CREATE INDEX IF NOT EXISTS idx_auction_lots_seller_id
    ON auction.lots(seller_id);

CREATE INDEX IF NOT EXISTS idx_auction_lots_winner_id
    ON auction.lots(winner_id);

CREATE INDEX IF NOT EXISTS idx_auction_lots_category_id
    ON auction.lots(category_id);

CREATE INDEX IF NOT EXISTS idx_auction_lots_status_id
    ON auction.lots(status_id);

CREATE INDEX IF NOT EXISTS idx_auction_lots_current_price
    ON auction.lots(current_price);

CREATE INDEX IF NOT EXISTS idx_auction_lots_end_time
    ON auction.lots(end_time);

CREATE INDEX IF NOT EXISTS idx_auction_lot_images_lot_id
    ON auction.lot_images(lot_id);

CREATE INDEX IF NOT EXISTS idx_auction_lot_status_history_lot_id
    ON auction.lot_status_history(lot_id);

CREATE INDEX IF NOT EXISTS idx_auction_lot_status_history_changed_by_user_id
    ON auction.lot_status_history(changed_by_user_id);

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
    ('Vehicles', 'Cars and other transport'),
    ('Books', 'Books and printed materials'),
    ('Clothing', 'Clothes and accessories'),
    ('Home', 'Home goods'),
    ('Other', 'Other goods')
    ON CONFLICT (name) DO NOTHING;