CREATE TABLE audit_logs (
    id            BIGSERIAL PRIMARY KEY,
    actor_user_id BIGINT,
    actor_email   VARCHAR(255) NOT NULL,
    action        VARCHAR(40) NOT NULL,
    target_type   VARCHAR(30) NOT NULL,
    target_id     BIGINT,
    description   VARCHAR(1000),
    occurred_at   TIMESTAMP NOT NULL DEFAULT now(),

    CONSTRAINT fk_audit_logs_actor
        FOREIGN KEY (actor_user_id) REFERENCES users (id) ON DELETE SET NULL
);

CREATE INDEX ix_audit_logs_occurred_at ON audit_logs (occurred_at DESC);
CREATE INDEX ix_audit_logs_actor ON audit_logs (actor_user_id, occurred_at DESC);
CREATE INDEX ix_audit_logs_target ON audit_logs (target_type, target_id, occurred_at DESC);

COMMENT ON TABLE audit_logs IS '논문·폴더·회원 관리 변경 이력';
