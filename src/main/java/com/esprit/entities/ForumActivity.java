package com.esprit.entities;

import java.time.LocalDateTime;

public class ForumActivity {
    private Integer id;
    private Integer postId;
    private String userKey;
    private String activityType;
    private String platform;
    private String reason;
    private LocalDateTime createdAt;

    public ForumActivity() {}

    public ForumActivity(Integer postId, String userKey, String activityType,
                    String platform, String reason, LocalDateTime createdAt) {
        this.postId = postId;
        this.userKey = userKey;
        this.activityType = activityType;
        this.platform = platform;
        this.reason = reason;
        this.createdAt = createdAt;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getPostId() { return postId; }
    public void setPostId(Integer postId) { this.postId = postId; }

    public String getUserKey() { return userKey; }
    public void setUserKey(String userKey) { this.userKey = userKey; }

    public String getActivityType() { return activityType; }
    public void setActivityType(String activityType) { this.activityType = activityType; }

    public String getPlatform() { return platform; }
    public void setPlatform(String platform) { this.platform = platform; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() {
        return "ForumActivity{" +
                "id=" + id +
                ", postId=" + postId +
                ", userKey='" + userKey + '\'' +
                ", activityType='" + activityType + '\'' +
                ", platform='" + platform + '\'' +
                ", reason='" + reason + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}

