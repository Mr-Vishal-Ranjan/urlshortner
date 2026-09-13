-- ─────────────────────────────────────────────────────────────────────────────
-- V1__create_urls_table.sql
-- Initial schema: creates the urls table.
-- Column lengths match the validation constraints in UrlConversionRequest.java
-- and the @Column(length=...) annotations in Url.java.
-- ─────────────────────────────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS urls (
    base64_hash  VARCHAR(7)    NOT NULL,
    original_url VARCHAR(2048) NOT NULL,
    user_id      VARCHAR(255)  NOT NULL,
    created_at   BIGINT        NOT NULL,
    updated_at   BIGINT        NOT NULL,
    CONSTRAINT pk_urls PRIMARY KEY (base64_hash)
);
