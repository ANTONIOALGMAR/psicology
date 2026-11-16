CREATE TABLE users (
    id UUID PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    email VARCHAR(180) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(30) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE invites (
    id UUID PRIMARY KEY,
    code VARCHAR(64) NOT NULL UNIQUE,
    email VARCHAR(180),
    role VARCHAR(30) NOT NULL,
    created_by UUID REFERENCES users(id),
    used_by UUID REFERENCES users(id),
    expires_at TIMESTAMP,
    used_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE patient_links (
    id UUID PRIMARY KEY,
    patient_id UUID NOT NULL REFERENCES users(id),
    psychologist_id UUID NOT NULL REFERENCES users(id),
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE (patient_id, psychologist_id)
);

CREATE TABLE journal_entries (
    id UUID PRIMARY KEY,
    patient_id UUID NOT NULL REFERENCES users(id),
    title VARCHAR(200),
    content TEXT NOT NULL,
    mood VARCHAR(40),
    entry_date TIMESTAMP NOT NULL DEFAULT NOW(),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE journal_attachments (
    id UUID PRIMARY KEY,
    entry_id UUID NOT NULL REFERENCES journal_entries(id) ON DELETE CASCADE,
    type VARCHAR(30) NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    original_name VARCHAR(255),
    mime_type VARCHAR(120),
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_journal_entries_patient_date ON journal_entries(patient_id, entry_date DESC);
CREATE INDEX idx_patient_links_patient ON patient_links(patient_id);
CREATE INDEX idx_patient_links_psych ON patient_links(psychologist_id);
