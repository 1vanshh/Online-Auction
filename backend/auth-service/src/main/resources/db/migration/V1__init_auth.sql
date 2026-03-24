CREATE TABLE auth.users (
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
    role VARCHAR(50) NOT NULL DEFAULT 'USER',
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    is_banned BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
    );

CREATE TABLE auth.user_bans (
                                              id BIGSERIAL PRIMARY KEY,
                                              user_id BIGINT NOT NULL,
                                              reason VARCHAR(255) NOT NULL,
    banned_from TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    banned_until TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_user_bans_user
    FOREIGN KEY (user_id) REFERENCES auth.users(id) ON DELETE CASCADE
    );

CREATE TABLE auth.refresh_tokens (
                                                   id BIGSERIAL PRIMARY KEY,
                                                   user_id BIGINT NOT NULL,
                                                   token VARCHAR(512) NOT NULL UNIQUE,
    expires_at TIMESTAMP NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_refresh_tokens_user
    FOREIGN KEY (user_id) REFERENCES auth.users(id) ON DELETE CASCADE
    );

CREATE INDEX idx_auth_users_email ON auth.users(email);
CREATE INDEX idx_auth_user_bans_user_id ON auth.user_bans(user_id);
CREATE INDEX idx_auth_refresh_tokens_user_id ON auth.refresh_tokens(user_id);

CREATE TABLE auth.audit_log (
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

CREATE INDEX idx_audit_log_user_id ON auth.audit_log(user_id);
CREATE INDEX idx_audit_log_created_at ON auth.audit_log(created_at);
