CREATE TABLE notifications (
    id              UUID         PRIMARY KEY,
    type            VARCHAR(20)  NOT NULL,
    medication_id   UUID         NOT NULL REFERENCES medications (id),
    medication_name VARCHAR(150) NOT NULL,
    quantity        INTEGER      NOT NULL,
    min_threshold   INTEGER,
    is_read         BOOLEAN      NOT NULL,
    created_at      TIMESTAMPTZ  NOT NULL
);

CREATE UNIQUE INDEX notifications_unread_medication_uq ON notifications (medication_id) WHERE NOT is_read;

CREATE INDEX notifications_created_at_idx ON notifications (created_at DESC);
