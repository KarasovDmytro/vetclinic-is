CREATE TABLE visits (
    id             UUID          PRIMARY KEY,
    appointment_id UUID          NOT NULL UNIQUE REFERENCES appointments (id),
    patient_id     UUID          NOT NULL REFERENCES patients (id),
    doctor_id      UUID          NOT NULL REFERENCES users (id),
    visit_date     TIMESTAMPTZ   NOT NULL,
    diagnosis      VARCHAR(2000) NOT NULL,
    prescriptions  VARCHAR(4000),
    notes          VARCHAR(4000),
    created_at     TIMESTAMPTZ   NOT NULL,
    updated_at     TIMESTAMPTZ   NOT NULL
);

CREATE INDEX visits_patient_visit_date_idx ON visits (patient_id, visit_date DESC);

CREATE TABLE visit_medications (
    visit_id        UUID         NOT NULL REFERENCES visits (id),
    position        INTEGER      NOT NULL,
    medication_id   UUID         NOT NULL REFERENCES medications (id),
    medication_name VARCHAR(150) NOT NULL,
    quantity        INTEGER      NOT NULL CHECK (quantity > 0),
    PRIMARY KEY (visit_id, position)
);
