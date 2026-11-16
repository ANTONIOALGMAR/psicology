CREATE TABLE tests (
    id UUID PRIMARY KEY,
    type VARCHAR(30) NOT NULL,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE test_questions (
    id UUID PRIMARY KEY,
    test_id UUID NOT NULL REFERENCES tests(id) ON DELETE CASCADE,
    position INT NOT NULL,
    prompt TEXT NOT NULL,
    kind VARCHAR(30) NOT NULL DEFAULT 'SCALE'
);

CREATE TABLE test_options (
    id UUID PRIMARY KEY,
    question_id UUID NOT NULL REFERENCES test_questions(id) ON DELETE CASCADE,
    label VARCHAR(255) NOT NULL,
    value INT NOT NULL
);

CREATE TABLE test_attempts (
    id UUID PRIMARY KEY,
    test_id UUID NOT NULL REFERENCES tests(id),
    patient_id UUID NOT NULL REFERENCES users(id),
    status VARCHAR(30) NOT NULL,
    score INT,
    result_summary TEXT,
    started_at TIMESTAMP NOT NULL DEFAULT NOW(),
    completed_at TIMESTAMP
);

CREATE TABLE test_answers (
    id UUID PRIMARY KEY,
    attempt_id UUID NOT NULL REFERENCES test_attempts(id) ON DELETE CASCADE,
    question_id UUID NOT NULL REFERENCES test_questions(id),
    option_id UUID REFERENCES test_options(id),
    text_answer TEXT,
    value INT
);

INSERT INTO tests (id, type, title, description, active) VALUES
('11111111-1111-1111-1111-111111111111', 'VOCATIONAL', 'Teste Vocacional Básico', 'Exploração inicial de interesses e aptidões profissionais.', TRUE),
('22222222-2222-2222-2222-222222222222', 'PSYCHOLOGICAL', 'Autoavaliação de Bem-estar', 'Questionário breve para acompanhamento de bem-estar emocional.', TRUE);
