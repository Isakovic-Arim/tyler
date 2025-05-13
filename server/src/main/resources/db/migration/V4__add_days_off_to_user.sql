CREATE TABLE days_off
(
    user_id     BIGINT NOT NULL,
    day_of_week VARCHAR(255)
);

ALTER TABLE app_user
    ADD days_off_per_week SMALLINT;

ALTER TABLE app_user
    ALTER COLUMN days_off_per_week SET NOT NULL;

ALTER TABLE days_off
    ADD CONSTRAINT fk_days_off_on_user FOREIGN KEY (user_id) REFERENCES app_user (id);