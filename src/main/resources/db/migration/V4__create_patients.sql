CREATE TABLE patients (
    id         UUID         PRIMARY KEY,
    owner_id   UUID         NOT NULL REFERENCES owners (id),
    name       VARCHAR(100) NOT NULL,
    species    VARCHAR(50)  NOT NULL,
    breed      VARCHAR(100),
    sex        VARCHAR(10)  NOT NULL,
    birth_date DATE,
    status     VARCHAR(10)  NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL,
    updated_at TIMESTAMPTZ  NOT NULL
);

CREATE INDEX patients_owner_id_idx ON patients (owner_id);
