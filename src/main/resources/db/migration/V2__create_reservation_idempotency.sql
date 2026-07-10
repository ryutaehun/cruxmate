CREATE TABLE reservation_idempotency
(
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    idempotency_key VARCHAR(255) NOT NULL,
    request_hash    CHAR(64)     NOT NULL,
    status          VARCHAR(20)  NOT NULL,
    created_at      DATETIME     NOT NULL,
    completed_at    DATETIME     NULL,
    member_id       BIGINT       NOT NULL,
    reservation_id  BIGINT       NULL,

    CONSTRAINT uk_reservation_idempotency_member_key
        UNIQUE (member_id, idempotency_key),

    CONSTRAINT uk_reservation_idempotency_reservation
        UNIQUE (reservation_id),

    CONSTRAINT fk_reservation_idempotency_member
        FOREIGN KEY (member_id) REFERENCES member (id),

    CONSTRAINT fk_reservation_idempotency_reservation
        FOREIGN KEY (reservation_id) REFERENCES reservation (id)
);