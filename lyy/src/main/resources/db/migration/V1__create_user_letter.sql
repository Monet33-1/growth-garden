-- Per-user idempotency and read completion state for preset letters.
CREATE TABLE user_letter (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    letter_id BIGINT NOT NULL,
    diary_id BIGINT NOT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_letter_user_letter (user_id, letter_id),
    UNIQUE KEY uk_user_letter_diary (diary_id),
    KEY idx_user_letter_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
