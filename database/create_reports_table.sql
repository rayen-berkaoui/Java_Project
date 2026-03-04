USE tabaani_db;

CREATE TABLE IF NOT EXISTS reports (
    id INT AUTO_INCREMENT PRIMARY KEY,
    post_id INT NULL,
    comment_id INT NULL,
    reporter_user_key VARCHAR(255) NOT NULL,
    reported_user_key VARCHAR(255) NULL,
    reason TEXT NOT NULL,
    content_type VARCHAR(50) NOT NULL,
    status VARCHAR(50) DEFAULT 'PENDING',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
