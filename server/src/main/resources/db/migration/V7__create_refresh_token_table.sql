CREATE TABLE refresh_tokens
(
    id         uuid PRIMARY KEY,
    user_id    bigint NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT NOW(),
    expires_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT fk_user FOREIGN KEY (user_id) REFERENCES app_user (id) ON DELETE CASCADE
);
CREATE INDEX refresh_tokens_user_id_idx ON refresh_tokens (user_id);