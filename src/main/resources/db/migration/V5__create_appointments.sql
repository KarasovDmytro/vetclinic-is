CREATE TABLE appointments (
    id         UUID        PRIMARY KEY,
    doctor_id  UUID        NOT NULL REFERENCES users (id),
    patient_id UUID        NOT NULL REFERENCES patients (id),
    starts_at  TIMESTAMPTZ NOT NULL,
    ends_at    TIMESTAMPTZ NOT NULL,
    status     VARCHAR(10) NOT NULL,
    visit_id   UUID,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE UNIQUE INDEX appointments_occupied_slot_uq ON appointments (doctor_id, starts_at)
    WHERE status IN ('SCHEDULED', 'COMPLETED');

CREATE INDEX appointments_patient_id_idx ON appointments (patient_id);
