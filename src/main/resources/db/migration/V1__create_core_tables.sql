CREATE TABLE member
(
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    email         VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role          VARCHAR(20)  NOT NULL,
    created_at    DATETIME     NOT NULL,
    updated_at    DATETIME     NOT NULL
);

CREATE TABLE climbing_session
(
    id                   BIGINT AUTO_INCREMENT PRIMARY KEY,
    title                VARCHAR(100) NOT NULL,
    location             VARCHAR(100) NOT NULL,
    start_at             DATETIME     NOT NULL,
    end_at               DATETIME     NOT NULL,
    reservation_open_at  DATETIME     NOT NULL,
    reservation_close_at DATETIME     NOT NULL,
    capacity             INT          NOT NULL,
    reserved_count       INT          NOT NULL DEFAULT 0,
    level                VARCHAR(20)  NOT NULL,
    status               VARCHAR(20)  NOT NULL DEFAULT 'SCHEDULED',
    created_at           DATETIME     NOT NULL,
    updated_at           DATETIME     NOT NULL,
    CONSTRAINT chk_session_capacity
        CHECK (capacity > 0),

    CONSTRAINT chk_session_reserved_count
        CHECK (
            reserved_count >= 0
                AND reserved_count <= capacity
            ),
    CONSTRAINT chk_session_time
        CHECK ( reservation_open_at < reservation_close_at
            AND reservation_close_at <= start_at
            AND start_at < end_at)
);

CREATE TABLE reservation
(
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    participant_count INT         NOT NULL,
    status            VARCHAR(20) NOT NULL default 'CONFIRMED',
    created_at        DATETIME    NOT NULL,
    canceled_at       DATETIME    NULL,
    session_id        BIGINT      NOT NULL,
    member_id         BIGINT      NOT NULL,

    constraint chk_reservation_participant_count
        CHECK ( participant_count >= 1
            AND participant_count <= 4),
    constraint fk_reservation_session
        foreign key (session_id) REFERENCES climbing_session (id),
    constraint fk_reservation_member
        foreign key (member_id) REFERENCES member (id)
);