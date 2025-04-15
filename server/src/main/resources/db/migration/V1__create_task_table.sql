CREATE SEQUENCE IF NOT EXISTS task_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE task
(
    id          BIGINT       NOT NULL,
    name        VARCHAR(255) NOT NULL,
    description VARCHAR(500),
    due_date    date,
    deadline    date         NOT NULL,
    xp          SMALLINT     NOT NULL,
    done        BOOLEAN      NOT NULL,
    CONSTRAINT pk_task PRIMARY KEY (id)
);