CREATE TABLE audit_logs (
    id UUID PRIMARY KEY,
    actor_id UUID REFERENCES users(id),
    actor_email VARCHAR(180),
    actor_role VARCHAR(30),
    action VARCHAR(80) NOT NULL,
    resource_type VARCHAR(80),
    resource_id VARCHAR(120),
    target_user_id UUID,
    details TEXT,
    ip_address VARCHAR(80),
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_audit_logs_created_at ON audit_logs(created_at DESC);
CREATE INDEX idx_audit_logs_actor ON audit_logs(actor_id);
CREATE INDEX idx_audit_logs_action ON audit_logs(action);
