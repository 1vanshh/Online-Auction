CREATE SCHEMA IF NOT EXISTS auth;

CREATE TABLE IF NOT EXISTS auth.users (
    id BIGSERIAL PRIMARY KEY,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
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
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_auth_users_role
        CHECK (role IN ('USER', 'ADMIN')),
    CONSTRAINT chk_auth_users_email
        CHECK (position('@' in email) > 1),
    CONSTRAINT chk_auth_users_first_name
        CHECK (char_length(trim(first_name)) BETWEEN 2 AND 100),
    CONSTRAINT chk_auth_users_last_name
        CHECK (char_length(trim(last_name)) BETWEEN 2 AND 100),
    CONSTRAINT chk_auth_users_birth_date
        CHECK (birth_date IS NULL OR birth_date <= CURRENT_DATE),
    CONSTRAINT chk_auth_users_not_active_and_banned
        CHECK (NOT (is_active = TRUE AND is_banned = TRUE))
);

CREATE TABLE IF NOT EXISTS auth.refresh_tokens (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    token VARCHAR(512) NOT NULL UNIQUE,
    expires_at TIMESTAMP NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_auth_refresh_tokens_user
        FOREIGN KEY (user_id) REFERENCES auth.users(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS auth.user_bans (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    reason VARCHAR(255) NOT NULL,
    banned_until TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by_user_id BIGINT,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT fk_auth_user_bans_user
        FOREIGN KEY (user_id) REFERENCES auth.users(id) ON DELETE CASCADE,
    CONSTRAINT fk_auth_user_bans_created_by
        FOREIGN KEY (created_by_user_id) REFERENCES auth.users(id) ON DELETE SET NULL,
    CONSTRAINT chk_auth_user_bans_reason
        CHECK (char_length(trim(reason)) BETWEEN 3 AND 255)
);

CREATE TABLE IF NOT EXISTS auth.audit_log (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT,
    action VARCHAR(100) NOT NULL,
    entity_type VARCHAR(100) NOT NULL,
    entity_id BIGINT,
    details TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_auth_audit_log_user
        FOREIGN KEY (user_id) REFERENCES auth.users(id) ON DELETE SET NULL,
    CONSTRAINT chk_auth_audit_log_action
        CHECK (char_length(trim(action)) BETWEEN 2 AND 100),
    CONSTRAINT chk_auth_audit_log_entity_type
        CHECK (char_length(trim(entity_type)) BETWEEN 2 AND 100)
);

CREATE INDEX IF NOT EXISTS idx_auth_users_email
    ON auth.users(email);

CREATE INDEX IF NOT EXISTS idx_auth_users_role
    ON auth.users(role);

CREATE INDEX IF NOT EXISTS idx_auth_users_is_active
    ON auth.users(is_active);

CREATE INDEX IF NOT EXISTS idx_auth_users_is_banned
    ON auth.users(is_banned);

CREATE INDEX IF NOT EXISTS idx_auth_refresh_tokens_user_id
    ON auth.refresh_tokens(user_id);

CREATE INDEX IF NOT EXISTS idx_auth_refresh_tokens_token
    ON auth.refresh_tokens(token);

CREATE INDEX IF NOT EXISTS idx_auth_refresh_tokens_revoked
    ON auth.refresh_tokens(revoked);

CREATE INDEX IF NOT EXISTS idx_auth_refresh_tokens_expires_at
    ON auth.refresh_tokens(expires_at);

CREATE INDEX IF NOT EXISTS idx_auth_user_bans_user_id
    ON auth.user_bans(user_id);

CREATE INDEX IF NOT EXISTS idx_auth_user_bans_active
    ON auth.user_bans(active);

CREATE INDEX IF NOT EXISTS idx_auth_user_bans_banned_until
    ON auth.user_bans(banned_until);

CREATE INDEX IF NOT EXISTS idx_auth_audit_log_user_id
    ON auth.audit_log(user_id);

CREATE INDEX IF NOT EXISTS idx_auth_audit_log_entity
    ON auth.audit_log(entity_type, entity_id);

CREATE INDEX IF NOT EXISTS idx_auth_audit_log_created_at
    ON auth.audit_log(created_at);
