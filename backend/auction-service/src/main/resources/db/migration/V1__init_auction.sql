CREATE TABLE auction.categories (
                                                  id BIGSERIAL PRIMARY KEY,
                                                  name VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(255)
    );

CREATE TABLE auction.lot_statuses (
                                                    id BIGSERIAL PRIMARY KEY,
                                                    code VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255)
    );

CREATE TABLE auction.lots (
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

CREATE TABLE auction.lot_images (
                                                  id BIGSERIAL PRIMARY KEY,
                                                  lot_id BIGINT NOT NULL,
                                                  image_url VARCHAR(500) NOT NULL,
    is_main BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_lot_images_lot
    FOREIGN KEY (lot_id) REFERENCES auction.lots(id) ON DELETE CASCADE
    );

CREATE TABLE auction.lot_status_history (
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

CREATE INDEX idx_auction_lots_seller_id ON auction.lots(seller_id);
CREATE INDEX idx_auction_lots_category_id ON auction.lots(category_id);
CREATE INDEX idx_auction_lots_status_id ON auction.lots(status_id);
CREATE INDEX idx_auction_lots_end_time ON auction.lots(end_time);

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