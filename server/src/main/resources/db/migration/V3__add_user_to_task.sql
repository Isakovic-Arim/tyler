CREATE SEQUENCE IF NOT EXISTS app_user_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE app_user
(
    id                 BIGINT  NOT NULL,
    current_xp         INTEGER NOT NULL,
    daily_xp_quota     INTEGER NOT NULL,
    current_streak     INTEGER NOT NULL,
    last_achieved_date date,
    CONSTRAINT pk_app_user PRIMARY KEY (id)
);

ALTER TABLE task
    ADD user_id BIGINT;

ALTER TABLE task
    ADD CONSTRAINT FK_TASK_ON_USER FOREIGN KEY (user_id) REFERENCES app_user (id);