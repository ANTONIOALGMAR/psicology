CREATE INDEX IF NOT EXISTS idx_test_attempts_patient ON test_attempts(patient_id, started_at DESC);
CREATE INDEX IF NOT EXISTS idx_test_attempts_patient_test_status ON test_attempts(patient_id, test_id, status);
CREATE INDEX IF NOT EXISTS idx_test_answers_attempt ON test_answers(attempt_id);
CREATE INDEX IF NOT EXISTS idx_journal_attachments_entry ON journal_attachments(entry_id);
CREATE INDEX IF NOT EXISTS idx_test_questions_test ON test_questions(test_id, position);
CREATE INDEX IF NOT EXISTS idx_test_options_question ON test_options(question_id);
