CREATE TABLE medications (
    id            UUID         PRIMARY KEY,
    name          VARCHAR(150) NOT NULL,
    quantity      INTEGER      NOT NULL CHECK (quantity >= 0),
    min_threshold INTEGER      CHECK (min_threshold >= 0),
    created_at    TIMESTAMPTZ  NOT NULL,
    updated_at    TIMESTAMPTZ  NOT NULL
);

CREATE UNIQUE INDEX medications_name_uq ON medications (upper(name));
