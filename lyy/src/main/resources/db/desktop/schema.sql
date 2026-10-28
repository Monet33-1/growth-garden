CREATE TABLE IF NOT EXISTS garden_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL,
    password VARCHAR(255) NOT NULL,
    nickname VARCHAR(50),
    avatar VARCHAR(255),
    birthday DATE,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_garden_user_username UNIQUE (username)
);

CREATE TABLE IF NOT EXISTS emotion (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    description VARCHAR(255),
    icon VARCHAR(255),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_emotion_name UNIQUE (name)
);

CREATE TABLE IF NOT EXISTS diary (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    title VARCHAR(100),
    content VARCHAR(100000) NOT NULL,
    mood VARCHAR(50),
    diary_date DATE,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_diary_user ON diary (user_id);

CREATE TABLE IF NOT EXISTS diary_emotion (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    diary_id BIGINT NOT NULL,
    emotion_id BIGINT NOT NULL,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_diary_emotion UNIQUE (diary_id, emotion_id),
    CONSTRAINT fk_diary_emotion_diary FOREIGN KEY (diary_id) REFERENCES diary (id) ON DELETE CASCADE,
    CONSTRAINT fk_diary_emotion_emotion FOREIGN KEY (emotion_id) REFERENCES emotion (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS garden (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    garden_name VARCHAR(100) NOT NULL,
    level INT DEFAULT 1,
    experience INT DEFAULT 0,
    stage VARCHAR(30) DEFAULT 'SEED',
    avatar VARCHAR(255),
    background VARCHAR(255),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_garden_user UNIQUE (user_id),
    CONSTRAINT fk_garden_user FOREIGN KEY (user_id) REFERENCES garden_user (id)
);

CREATE TABLE IF NOT EXISTS letter (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(100) NOT NULL,
    content VARCHAR(100000) NOT NULL,
    unlock_type VARCHAR(30) NOT NULL,
    unlock_value INT NOT NULL,
    cover VARCHAR(255),
    sort INT DEFAULT 0,
    create_time TIMESTAMP,
    update_time TIMESTAMP
);

CREATE TABLE IF NOT EXISTS user_letter (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    letter_id BIGINT NOT NULL,
    diary_id BIGINT NOT NULL,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_user_letter_user_letter UNIQUE (user_id, letter_id),
    CONSTRAINT uk_user_letter_diary UNIQUE (diary_id)
);
