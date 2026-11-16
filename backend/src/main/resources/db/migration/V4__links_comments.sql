CREATE TABLE journal_comments (
    id UUID PRIMARY KEY,
    entry_id UUID NOT NULL REFERENCES journal_entries(id) ON DELETE CASCADE,
    author_id UUID NOT NULL REFERENCES users(id),
    content TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_journal_comments_entry ON journal_comments(entry_id, created_at);
