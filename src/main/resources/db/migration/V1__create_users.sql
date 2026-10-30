CREATE TABLE users (
    id         UUID         PRIMARY KEY,
    email      VARCHAR(254) NOT NULL,
    full_name  VARCHAR(150) NOT NULL,
    role       VARCHAR(20)  NOT NULL,
    active     BOOLEAN      NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL
);
