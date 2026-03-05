-- =====================================================
-- Forum Integration Migration Script
-- Run against the 'tabaany' database
-- =====================================================

USE tabaany;

-- Posts table
CREATE TABLE IF NOT EXISTS posts (
    id INT AUTO_INCREMENT PRIMARY KEY,
    content LONGTEXT NULL DEFAULT NULL,
    image_path LONGTEXT NULL DEFAULT NULL,
    video_path LONGTEXT NULL DEFAULT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Comments table
CREATE TABLE IF NOT EXISTS comments (
    id INT AUTO_INCREMENT PRIMARY KEY,
    post_id INT NOT NULL,
    user_key VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    likes_count INT DEFAULT 0,
    dislikes_count INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL DEFAULT NULL,
    INDEX idx_comments_post_id (post_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Activities table (reactions: LIKE, DISLIKE, SHARE, REPORT)
CREATE TABLE IF NOT EXISTS activities (
    id INT AUTO_INCREMENT PRIMARY KEY,
    post_id INT NULL,
    comment_id INT NULL,
    user_key VARCHAR(255) NOT NULL,
    activity_type VARCHAR(50) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_activities_post_id (post_id),
    INDEX idx_activities_comment_id (comment_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Shares table
CREATE TABLE IF NOT EXISTS shares (
    id INT AUTO_INCREMENT PRIMARY KEY,
    post_id INT NOT NULL,
    user_key VARCHAR(255) NOT NULL,
    platform VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_shares_post_id (post_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Reports table
CREATE TABLE IF NOT EXISTS reports (
    id INT AUTO_INCREMENT PRIMARY KEY,
    post_id INT NULL,
    comment_id INT NULL,
    reporter_user_key VARCHAR(255) NOT NULL,
    reported_user_key VARCHAR(255) NULL,
    reason TEXT NOT NULL,
    content_type VARCHAR(50) NOT NULL,
    status VARCHAR(50) DEFAULT 'PENDING',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_reports_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- User warnings table (3-strike system)
CREATE TABLE IF NOT EXISTS user_warnings (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_key VARCHAR(100) NOT NULL,
    user_email VARCHAR(255),
    warning_count INT NOT NULL DEFAULT 0,
    last_bad_word VARCHAR(100),
    blocked_until DATETIME NULL,
    last_warning_at DATETIME,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY unique_user_key (user_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Warning history table
CREATE TABLE IF NOT EXISTS warning_history (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_key VARCHAR(100) NOT NULL,
    warning_level INT NOT NULL,
    bad_word VARCHAR(100),
    post_content TEXT,
    action_taken VARCHAR(50) NOT NULL COMMENT 'WARNING_1, WARNING_2, BLOCKED',
    email_sent BOOLEAN DEFAULT FALSE,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_warning_history_user_key (user_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Chat violations table (auto-created by ForumChatModerationService but defined here for completeness)
CREATE TABLE IF NOT EXISTS chat_violations (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(100) NOT NULL,
    message TEXT,
    bad_word VARCHAR(100),
    status VARCHAR(20) DEFAULT 'PENDING',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Chat bans table
CREATE TABLE IF NOT EXISTS chat_bans (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(100) NOT NULL UNIQUE,
    reason VARCHAR(255),
    banned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    banned_until DATETIME NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Chat messages table
CREATE TABLE IF NOT EXISTS chat_messages (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(100) NOT NULL,
    message TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
