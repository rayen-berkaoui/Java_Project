package org.example.entities;

import java.time.LocalDateTime;

public class UserWarning {

    private int id;
    private String userKey;
    private String userEmail;
    private int warningCount;
    private String lastBadWord;
    private LocalDateTime blockedUntil;
    private LocalDateTime lastWarningAt;
    private LocalDateTime createdAt;

    public UserWarning() {}

    public UserWarning(String userKey, String userEmail) {
        this.userKey = userKey;
        this.userEmail = userEmail;
        this.warningCount = 0;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getUserKey() { return userKey; }
    public void setUserKey(String userKey) { this.userKey = userKey; }

    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }

    public int getWarningCount() { return warningCount; }
    public void setWarningCount(int warningCount) { this.warningCount = warningCount; }

    public String getLastBadWord() { return lastBadWord; }
    public void setLastBadWord(String lastBadWord) { this.lastBadWord = lastBadWord; }

    public LocalDateTime getBlockedUntil() { return blockedUntil; }
    public void setBlockedUntil(LocalDateTime blockedUntil) { this.blockedUntil = blockedUntil; }

    public LocalDateTime getLastWarningAt() { return lastWarningAt; }
    public void setLastWarningAt(LocalDateTime lastWarningAt) { this.lastWarningAt = lastWarningAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public boolean isBlocked() {
        return blockedUntil != null && LocalDateTime.now().isBefore(blockedUntil);
    }

    public int getAlertLevel() {
        return warningCount + 1;
    }

    @Override
    public String toString() {
        return "UserWarning{" +
                "userKey='" + userKey + '\'' +
                ", warningCount=" + warningCount +
                ", blocked=" + isBlocked() +
                ", lastBadWord='" + lastBadWord + '\'' +
                '}';
    }
}
