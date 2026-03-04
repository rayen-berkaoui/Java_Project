package org.example.services;

import org.example.entities.UserWarning;
import org.example.utils.Db;

import java.sql.*;
import java.time.LocalDateTime;

public class WarningService {

    private static final int BASE_BLOCK_HOURS = 24;
    private final Connection con;
    private final EmailService emailService;

    public WarningService() {
        this.con = Db.getInstance().getConnection();
        this.emailService = new EmailService();
        ensureTablesExist();
    }

    private void ensureTablesExist() {
        try (Statement st = con.createStatement()) {
            st.executeUpdate("""
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
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """);
            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS warning_history (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    user_key VARCHAR(100) NOT NULL,
                    warning_level INT NOT NULL,
                    bad_word VARCHAR(100),
                    post_content TEXT,
                    action_taken VARCHAR(50) NOT NULL,
                    email_sent BOOLEAN DEFAULT FALSE,
                    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                    INDEX idx_user_key (user_key)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """);
        } catch (SQLException e) {
            System.err.println("[WarningService] Tables check: " + e.getMessage());
        }
    }

    public UserWarning getOrCreate(String userKey, String userEmail) throws SQLException {
        String selectSql = "SELECT * FROM user_warnings WHERE user_key = ?";
        try (PreparedStatement ps = con.prepareStatement(selectSql)) {
            ps.setString(1, userKey);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    UserWarning uw = mapFromResultSet(rs);

                    if (userEmail != null && !userEmail.equals(uw.getUserEmail())) {
                        updateEmail(userKey, userEmail);
                        uw.setUserEmail(userEmail);
                    }
                    return uw;
                }
            }
        }

        String insertSql = "INSERT INTO user_warnings (user_key, user_email, warning_count) VALUES (?, ?, 0)";
        try (PreparedStatement ps = con.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, userKey);
            ps.setString(2, userEmail);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                UserWarning uw = new UserWarning(userKey, userEmail);
                if (rs.next()) uw.setId(rs.getInt(1));
                return uw;
            }
        }
    }

    private void updateEmail(String userKey, String email) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("UPDATE user_warnings SET user_email = ? WHERE user_key = ?")) {
            ps.setString(1, email);
            ps.setString(2, userKey);
            ps.executeUpdate();
        }
    }

    public LocalDateTime checkBlocked(String userKey) throws SQLException {
        String sql = "SELECT blocked_until FROM user_warnings WHERE user_key = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, userKey);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Timestamp ts = rs.getTimestamp("blocked_until");
                    if (ts != null) {
                        LocalDateTime blockedUntil = ts.toLocalDateTime();
                        if (LocalDateTime.now().isBefore(blockedUntil)) {
                            return blockedUntil;
                        }
                    }
                }
            }
        }
        return null;
    }

    public WarningResult processViolation(String userKey, String userEmail, String badWord, String postContent) {
        try {
            UserWarning uw = getOrCreate(userKey, userEmail);
            int newCount = uw.getWarningCount() + 1;

            LocalDateTime blockedUntil = null;
            int blockHours = 0;
            if (newCount >= 3) {
                blockHours = BASE_BLOCK_HOURS * (newCount - 2);
                blockedUntil = LocalDateTime.now().plusHours(blockHours);
            }

            String updateSql = "UPDATE user_warnings SET warning_count = ?, last_bad_word = ?, blocked_until = ?, last_warning_at = NOW() WHERE user_key = ?";
            try (PreparedStatement ps = con.prepareStatement(updateSql)) {
                ps.setInt(1, newCount);
                ps.setString(2, badWord);
                ps.setTimestamp(3, blockedUntil != null ? Timestamp.valueOf(blockedUntil) : null);
                ps.setString(4, userKey);
                ps.executeUpdate();
            }

            String action;
            boolean emailSent = false;
            String alertMessage;

            if (newCount == 1) {
                action = "WARNING_1";
                emailSent = emailService.sendWarning1(userEmail, userKey, badWord);
                alertMessage = "⚠️ Alerte 1 : Premier avertissement. Un email vous a été envoyé.";
            } else if (newCount == 2) {
                action = "WARNING_2";
                emailSent = emailService.sendWarning2(userEmail, userKey, badWord);
                alertMessage = "⚠️⚠️ Alerte 2 : Deuxième avertissement ! Prochaine infraction = blocage.";
            } else {
                action = "BLOCKED";
                emailSent = emailService.sendBlockNotification(userEmail, userKey, badWord, blockHours);
                alertMessage = "🚫 Compte bloqué pour " + blockHours + "h. Un email a été envoyé.";
            }

            logWarningHistory(userKey, newCount, badWord, postContent, action, emailSent);

            System.out.println("[WarningService] " + userKey + " → " + action +
                    " (warning #" + newCount + ") bad_word=" + badWord + " email_sent=" + emailSent);

            return new WarningResult(action, newCount, alertMessage, emailSent, blockedUntil);

        } catch (SQLException e) {
            System.err.println("[WarningService] Erreur SQL: " + e.getMessage());
            return new WarningResult("ERROR", 0, "Erreur interne du système de modération.", false, null);
        }
    }

    private void logWarningHistory(String userKey, int level, String badWord, String postContent, String action, boolean emailSent) {
        String sql = "INSERT INTO warning_history (user_key, warning_level, bad_word, post_content, action_taken, email_sent) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, userKey);
            ps.setInt(2, level);
            ps.setString(3, badWord);
            ps.setString(4, postContent != null && postContent.length() > 500 ? postContent.substring(0, 500) : postContent);
            ps.setString(5, action);
            ps.setBoolean(6, emailSent);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[WarningService] History log error: " + e.getMessage());
        }
    }

    private UserWarning mapFromResultSet(ResultSet rs) throws SQLException {
        UserWarning uw = new UserWarning();
        uw.setId(rs.getInt("id"));
        uw.setUserKey(rs.getString("user_key"));
        uw.setUserEmail(rs.getString("user_email"));
        uw.setWarningCount(rs.getInt("warning_count"));
        uw.setLastBadWord(rs.getString("last_bad_word"));
        Timestamp blocked = rs.getTimestamp("blocked_until");
        if (blocked != null) uw.setBlockedUntil(blocked.toLocalDateTime());
        Timestamp lastWarn = rs.getTimestamp("last_warning_at");
        if (lastWarn != null) uw.setLastWarningAt(lastWarn.toLocalDateTime());
        Timestamp created = rs.getTimestamp("created_at");
        if (created != null) uw.setCreatedAt(created.toLocalDateTime());
        return uw;
    }

    public static class WarningResult {
        private final String action;
        private final int warningCount;
        private final String alertMessage;
        private final boolean emailSent;
        private final LocalDateTime blockedUntil;

        public WarningResult(String action, int warningCount, String alertMessage, boolean emailSent, LocalDateTime blockedUntil) {
            this.action = action;
            this.warningCount = warningCount;
            this.alertMessage = alertMessage;
            this.emailSent = emailSent;
            this.blockedUntil = blockedUntil;
        }

        public String getAction() { return action; }
        public int getWarningCount() { return warningCount; }
        public String getAlertMessage() { return alertMessage; }
        public boolean isEmailSent() { return emailSent; }
        public LocalDateTime getBlockedUntil() { return blockedUntil; }
        public boolean isBlocked() { return "BLOCKED".equals(action); }
    }
}
