-- Evolução 11: agendamentos de atendimento com isolamento por psicólogo
CREATE TABLE appointments (
    id UUID PRIMARY KEY,
    patient_id UUID NOT NULL REFERENCES users(id),
    psychologist_id UUID NOT NULL REFERENCES users(id),
    starts_at TIMESTAMP NOT NULL,
    ends_at TIMESTAMP NOT NULL,
    mode VARCHAR(20) NOT NULL DEFAULT 'ONLINE',
    status VARCHAR(20) NOT NULL DEFAULT 'SCHEDULED',
    title VARCHAR(200),
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_appointment_period CHECK (ends_at > starts_at)
);

CREATE INDEX idx_appointments_psych_start ON appointments(psychologist_id, starts_at);
CREATE INDEX idx_appointments_patient_start ON appointments(patient_id, starts_at);
CREATE INDEX idx_appointments_status ON appointments(status);
