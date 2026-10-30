CREATE TABLE owners (
    id         UUID         PRIMARY KEY,
    full_name  VARCHAR(150) NOT NULL,
    phone      VARCHAR(16)  NOT NULL,
    email      VARCHAR(254),
    address    VARCHAR(300),
    created_at TIMESTAMPTZ  NOT NULL,
    updated_at TIMESTAMPTZ  NOT NULL
);
