-- Table pour le système d'alertes automatiques (3-strike)
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

-- Table historique détaillé des infractions
CREATE TABLE IF NOT EXISTS warning_history (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_key VARCHAR(100) NOT NULL,
    warning_level INT NOT NULL,
    bad_word VARCHAR(100),
    post_content TEXT,
    action_taken VARCHAR(50) NOT NULL COMMENT 'WARNING_1, WARNING_2, BLOCKED',
    email_sent BOOLEAN DEFAULT FALSE,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_key (user_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
