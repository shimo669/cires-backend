-- SQL Migration Script for CIRES Backend Refactoring
-- This script creates the new database structure with proper relationships

-- Drop old tables if they exist (be careful with production!)
-- DROP TABLE IF EXISTS complaints;
-- DROP TABLE IF EXISTS reports;
-- DROP TABLE IF EXISTS user;
-- DROP TABLE IF EXISTS role;

-- Create Role table
CREATE TABLE IF NOT EXISTS role (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    role_name VARCHAR(50) NOT NULL UNIQUE
);

-- Create GovernmentLevel table
CREATE TABLE IF NOT EXISTS government_level (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    level_name VARCHAR(255) NOT NULL,
    level_type ENUM('VILLAGE', 'CELL', 'SECTOR', 'DISTRICT', 'PROVINCE', 'NATIONAL') NOT NULL,
    parent_level_id BIGINT,
    FOREIGN KEY (parent_level_id) REFERENCES government_level(id) ON DELETE SET NULL
);

-- Create User table
CREATE TABLE IF NOT EXISTS user (
    user_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(100) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    nationalId VARCHAR(50) NOT NULL UNIQUE,
    location_id BIGINT,
    role_id BIGINT NOT NULL,
    FOREIGN KEY (location_id) REFERENCES government_level(id) ON DELETE SET NULL,
    FOREIGN KEY (role_id) REFERENCES role(id) ON DELETE RESTRICT
);

-- Create Category table
CREATE TABLE IF NOT EXISTS category (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    category_name VARCHAR(255) NOT NULL UNIQUE,
    description TEXT
);

-- Create Reports table
CREATE TABLE IF NOT EXISTS reports (
    report_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    status ENUM('PENDING', 'IN_PROGRESS', 'RESOLVED', 'ESCALATED') NOT NULL DEFAULT 'PENDING',
    category_id BIGINT NOT NULL,
    reporter_id BIGINT NOT NULL,
    current_level_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    sla_deadline TIMESTAMP NOT NULL,
    FOREIGN KEY (category_id) REFERENCES category(id) ON DELETE RESTRICT,
    FOREIGN KEY (reporter_id) REFERENCES user(user_id) ON DELETE CASCADE,
    FOREIGN KEY (current_level_id) REFERENCES government_level(id) ON DELETE RESTRICT
);

-- Create SlaConfig table
CREATE TABLE IF NOT EXISTS sla_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    category_id BIGINT NOT NULL,
    government_level_id BIGINT NOT NULL,
    duration_hours INT NOT NULL,
    UNIQUE KEY unique_sla (category_id, government_level_id),
    FOREIGN KEY (category_id) REFERENCES category(id) ON DELETE CASCADE,
    FOREIGN KEY (government_level_id) REFERENCES government_level(id) ON DELETE CASCADE
);

-- Insert default roles
INSERT INTO role (role_name) VALUES ('CITIZEN'), ('LEADER'), ('ADMIN') ON DUPLICATE KEY UPDATE role_name = role_name;

-- Insert sample government levels (Rwanda hierarchy example)
INSERT INTO government_level (level_name, level_type, parent_level_id) VALUES
('Rwanda', 'NATIONAL', NULL),
('Eastern Province', 'PROVINCE', 1),
('Bugesera District', 'DISTRICT', 2),
('Nyarugenge Sector', 'SECTOR', 3),
('Kacyiru Cell', 'CELL', 4),
('Kacyiru Village', 'VILLAGE', 5);

-- Insert sample categories
INSERT INTO category (category_name, description) VALUES
('Road Infrastructure', 'Issues related to road quality, potholes, and infrastructure'),
('Water & Sanitation', 'Issues related to water supply and sanitation services'),
('Health Services', 'Issues related to health facilities and medical services'),
('Education', 'Issues related to schools and educational facilities'),
('Security', 'Issues related to public security and safety');

-- Create indexes for performance
CREATE INDEX idx_user_role ON user(role_id);
CREATE INDEX idx_user_location ON user(location_id);
CREATE INDEX idx_report_reporter ON reports(reporter_id);
CREATE INDEX idx_report_category ON reports(category_id);
CREATE INDEX idx_report_level ON reports(current_level_id);
CREATE INDEX idx_report_status ON reports(status);
CREATE INDEX idx_government_level_parent ON government_level(parent_level_id);

