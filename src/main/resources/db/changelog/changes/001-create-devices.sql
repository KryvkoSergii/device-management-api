--liquibase formatted sql

--changeset device-api:001
CREATE TABLE devices (
    id UUID PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    brand VARCHAR(100) NOT NULL,
    state VARCHAR(20) NOT NULL CHECK (state IN ('AVAILABLE', 'IN_USE', 'INACTIVE')),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_devices_brand_lower ON devices (lower(brand));
CREATE INDEX idx_devices_state ON devices (state);

--rollback DROP TABLE devices;
