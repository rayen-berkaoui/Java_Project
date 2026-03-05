package com.esprit.services;

import com.esprit.utils.MyDataBase;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class ForumChatModerationService {

    private final Connection con;
    private final ForumContentModerationService moderationService;

    public ForumChatModerationService() {
        this.con = MyDataBase.getInstance().getConnection();
        this.moderationService = new ForumContentModerationService();
        ensureTablesExist();
    }

    private void ensureTablesExist() {
        try (Statement st = con.createStatement()) {
            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS chat_violations (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    username VARCHAR(100) NOT NULL,
                    message TEXT NOT NULL,
                    bad_word VARCHAR(100),
                    status ENUM('PENDING', 'RESOLVED', 'BANNED') NOT NULL DEFAULT 'PENDING',
                    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                    INDEX idx_status (status),
                    INDEX idx_username (username)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """);
            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS chat_bans (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    username VARCHAR(100) NOT NULL,
                    reason VARCHAR(255),
                    banned_until DATETIME NOT NULL,
                    banned_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                    banned_by VARCHAR(50) DEFAULT 'admin',
                    UNIQUE KEY unique_username (username),
                    INDEX idx_banned_until (banned_until)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """);
            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS chat_messages (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    username VARCHAR(100) NOT NULL,
                    message TEXT NOT NULL,
                    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                    INDEX idx_created_at (created_at)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """);
        } catch (SQLException e) {
            System.err.println("[ForumChatModerationService] Tables check: " + e.getMessage());
        }
    }

    public void saveMessage(String username, String message) {
        String sql = "INSERT INTO chat_messages (username, message) VALUES (?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, message != null && message.length() > 2000 ? message.substring(0, 2000) : message);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[ChatModeration] saveMessage: " + e.getMessage());
        }
    }

    public List<ChatMessage> getRecentMessages(int limit) {
        List<ChatMessage> list = new ArrayList<>();
        String sql = "SELECT * FROM chat_messages ORDER BY created_at DESC LIMIT ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapMessage(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[ChatModeration] getRecentMessages: " + e.getMessage());
        }

        java.util.Collections.reverse(list);
        return list;
    }

    public void deleteMessage(int messageId) throws SQLException {
        String sql = "DELETE FROM chat_messages WHERE id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, messageId);
            ps.executeUpdate();
        }
    }

    public void clearAllMessages() throws SQLException {
        try (Statement st = con.createStatement()) {
            st.executeUpdate("DELETE FROM chat_messages");
        }
    }

    private ChatMessage mapMessage(ResultSet rs) throws SQLException {
        ChatMessage m = new ChatMessage();
        m.setId(rs.getInt("id"));
        m.setUsername(rs.getString("username"));
        m.setMessage(rs.getString("message"));
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) m.setCreatedAt(ts.toLocalDateTime());
        return m;
    }

    public String checkMessage(String username, String message) {
        if (message == null || message.isBlank()) return null;

        String badWord = moderationService.findFirstBadWord(message);
        if (badWord != null) {
            logViolation(username, message, badWord);
            return badWord;
        }
        return null;
    }

    private void logViolation(String username, String message, String badWord) {
        String sql = "INSERT INTO chat_violations (username, message, bad_word) VALUES (?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, message.length() > 1000 ? message.substring(0, 1000) : message);
            ps.setString(3, badWord);
            ps.executeUpdate();
            System.out.println("[ChatModeration] Violation logged: " + username + " → " + badWord);
        } catch (SQLException e) {
            System.err.println("[ChatModeration] Log error: " + e.getMessage());
        }
    }

    public List<ChatViolation> getAllViolations() {
        List<ChatViolation> list = new ArrayList<>();
        String sql = "SELECT * FROM chat_violations ORDER BY created_at DESC LIMIT 200";
        try (PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapViolation(rs));
            }
        } catch (SQLException e) {
            System.err.println("[ChatModeration] getAllViolations: " + e.getMessage());
        }
        return list;
    }

    public List<ChatViolation> getPendingViolations() {
        List<ChatViolation> list = new ArrayList<>();
        String sql = "SELECT * FROM chat_violations WHERE status = 'PENDING' ORDER BY created_at DESC";
        try (PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapViolation(rs));
            }
        } catch (SQLException e) {
            System.err.println("[ChatModeration] getPendingViolations: " + e.getMessage());
        }
        return list;
    }

    public int countPendingViolations() {
        String sql = "SELECT COUNT(*) FROM chat_violations WHERE status = 'PENDING'";
        try (PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            System.err.println("[ChatModeration] countPending: " + e.getMessage());
        }
        return 0;
    }

    public void banUser(String username, int hours, String reason) throws SQLException {
        LocalDateTime until = LocalDateTime.now().plusHours(hours);
        String sql = """
            INSERT INTO chat_bans (username, reason, banned_until)
            VALUES (?, ?, ?)
            ON DUPLICATE KEY UPDATE reason = VALUES(reason), banned_until = VALUES(banned_until), banned_at = NOW()
        """;
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, reason != null ? reason : "Langage inapproprié dans le chat");
            ps.setTimestamp(3, Timestamp.valueOf(until));
            ps.executeUpdate();
        }

        String updateSql = "UPDATE chat_violations SET status = 'BANNED' WHERE username = ? AND status = 'PENDING'";
        try (PreparedStatement ps = con.prepareStatement(updateSql)) {
            ps.setString(1, username);
            ps.executeUpdate();
        }
        System.out.println("[ChatModeration] BANNED: " + username + " until " + until);
    }

    public void unbanUser(String username) throws SQLException {
        String sql = "DELETE FROM chat_bans WHERE username = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.executeUpdate();
        }
    }

    public LocalDateTime checkBanned(String username) {
        String sql = "SELECT banned_until FROM chat_bans WHERE username = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Timestamp ts = rs.getTimestamp("banned_until");
                    if (ts != null) {
                        LocalDateTime until = ts.toLocalDateTime();
                        if (LocalDateTime.now().isBefore(until)) {
                            return until;
                        } else {

                            unbanUser(username);
                        }
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("[ChatModeration] checkBanned: " + e.getMessage());
        }
        return null;
    }

    public void markResolved(int violationId) throws SQLException {
        String sql = "UPDATE chat_violations SET status = 'RESOLVED' WHERE id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, violationId);
            ps.executeUpdate();
        }
    }

    public List<ChatBan> getActiveBans() {
        List<ChatBan> bans = new ArrayList<>();
        String sql = "SELECT * FROM chat_bans WHERE banned_until > NOW() ORDER BY banned_at DESC";
        try (PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                bans.add(mapBan(rs));
            }
        } catch (SQLException e) {
            System.err.println("[ChatModeration] getActiveBans: " + e.getMessage());
        }
        return bans;
    }

    public List<ChatBan> getAllBans() {
        List<ChatBan> bans = new ArrayList<>();
        String sql = "SELECT * FROM chat_bans ORDER BY banned_at DESC";
        try (PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                bans.add(mapBan(rs));
            }
        } catch (SQLException e) {
            System.err.println("[ChatModeration] getAllBans: " + e.getMessage());
        }
        return bans;
    }

    public void extendBan(String username, int extraHours) throws SQLException {
        String sql = "UPDATE chat_bans SET banned_until = DATE_ADD(banned_until, INTERVAL ? HOUR) WHERE username = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, extraHours);
            ps.setString(2, username);
            ps.executeUpdate();
        }
    }

    private ChatBan mapBan(ResultSet rs) throws SQLException {
        ChatBan b = new ChatBan();
        b.setId(rs.getInt("id"));
        b.setUsername(rs.getString("username"));
        b.setReason(rs.getString("reason"));
        Timestamp bannedAt = rs.getTimestamp("banned_at");
        if (bannedAt != null) b.setBannedAt(bannedAt.toLocalDateTime());
        Timestamp bannedUntil = rs.getTimestamp("banned_until");
        if (bannedUntil != null) b.setBannedUntil(bannedUntil.toLocalDateTime());
        b.setBannedBy(rs.getString("banned_by"));
        return b;
    }

    private ChatViolation mapViolation(ResultSet rs) throws SQLException {
        ChatViolation v = new ChatViolation();
        v.setId(rs.getInt("id"));
        v.setUsername(rs.getString("username"));
        v.setMessage(rs.getString("message"));
        v.setBadWord(rs.getString("bad_word"));
        v.setStatus(rs.getString("status"));
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) v.setCreatedAt(ts.toLocalDateTime());
        return v;
    }

    public static class ChatViolation {
        private int id;
        private String username;
        private String message;
        private String badWord;
        private String status;
        private LocalDateTime createdAt;

        public int getId() { return id; }
        public void setId(int id) { this.id = id; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public String getBadWord() { return badWord; }
        public void setBadWord(String badWord) { this.badWord = badWord; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

        public String getFormattedDate() {
            if (createdAt == null) return "";
            return createdAt.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
        }
    }

    public static class ChatMessage {
        private int id;
        private String username;
        private String message;
        private LocalDateTime createdAt;

        public int getId() { return id; }
        public void setId(int id) { this.id = id; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

        public String getFormattedTime() {
            if (createdAt == null) return "";
            return createdAt.format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        }
    }

    public static class ChatBan {
        private int id;
        private String username;
        private String reason;
        private LocalDateTime bannedAt;
        private LocalDateTime bannedUntil;
        private String bannedBy;

        public int getId() { return id; }
        public void setId(int id) { this.id = id; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
        public LocalDateTime getBannedAt() { return bannedAt; }
        public void setBannedAt(LocalDateTime bannedAt) { this.bannedAt = bannedAt; }
        public LocalDateTime getBannedUntil() { return bannedUntil; }
        public void setBannedUntil(LocalDateTime bannedUntil) { this.bannedUntil = bannedUntil; }
        public String getBannedBy() { return bannedBy; }
        public void setBannedBy(String bannedBy) { this.bannedBy = bannedBy; }

        public String getFormattedBannedAt() {
            if (bannedAt == null) return "";
            return bannedAt.format(DateTimeFormatter.ofPattern("dd/MM HH:mm"));
        }

        public String getFormattedBannedUntil() {
            if (bannedUntil == null) return "";
            return bannedUntil.format(DateTimeFormatter.ofPattern("dd/MM HH:mm"));
        }

        public boolean isActive() {
            return bannedUntil != null && LocalDateTime.now().isBefore(bannedUntil);
        }

        public String getRemainingTime() {
            if (bannedUntil == null) return "";
            if (!isActive()) return "Expiré";
            long minutes = java.time.Duration.between(LocalDateTime.now(), bannedUntil).toMinutes();
            if (minutes < 60) return minutes + " min";
            long hours = minutes / 60;
            if (hours < 24) return hours + "h " + (minutes % 60) + "m";
            return (hours / 24) + "j " + (hours % 24) + "h";
        }
    }
}


