ALTER TABLE task
    ADD remaining_xp SMALLINT;

ALTER TABLE task
    ALTER COLUMN remaining_xp SET NOT NULL;

ALTER TABLE refresh_tokens
    ALTER COLUMN created_at SET NOT NULL;