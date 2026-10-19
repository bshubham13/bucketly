-- Bucketly Database Schema (MySQL 8.0+)
-- Database: bucketly

USE bucketly;

-- 1. Dreams Table
CREATE TABLE IF NOT EXISTS dreams (
    id BIGINT NOT NULL AUTO_INCREMENT,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    category VARCHAR(100) NOT NULL,
    target_date VARCHAR(100),
    status VARCHAR(50) NOT NULL DEFAULT 'Not Started',
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. Memories Table (Memory Wall Milestone 1)
-- One-to-one relationship with dreams (each completed dream has at most one memory)
CREATE TABLE IF NOT EXISTS memories (
    id BIGINT NOT NULL AUTO_INCREMENT,
    dream_id BIGINT NOT NULL,
    completed_date DATE NOT NULL,
    story TEXT,
    photo_path VARCHAR(255),
    original_filename VARCHAR(255),
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_memories_dream_id (dream_id),
    CONSTRAINT fk_memories_dream FOREIGN KEY (dream_id) REFERENCES dreams (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
