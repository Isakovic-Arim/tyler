CREATE TABLE roles
(
    user_id BIGINT NOT NULL,
    role    VARCHAR(255)
);

ALTER TABLE app_user
    ADD password_hash VARCHAR(255);

ALTER TABLE app_user
    ADD username VARCHAR(255);

ALTER TABLE app_user
    ALTER COLUMN password_hash SET NOT NULL;

ALTER TABLE app_user
    ALTER COLUMN username SET NOT NULL;

ALTER TABLE app_user
    ADD CONSTRAINT uc_app_user_username UNIQUE (username);

CREATE UNIQUE INDEX user_username_idx ON app_user (username);

ALTER TABLE roles
    ADD CONSTRAINT fk_roles_on_user FOREIGN KEY (user_id) REFERENCES app_user (id);