-- Bucketly Database Schema (MySQL 8.0+)
-- Database: bucketly

USE bucketly;

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

