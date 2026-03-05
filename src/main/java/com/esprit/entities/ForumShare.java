package com.esprit.entities;

import java.time.LocalDateTime;

public class ForumShare {
    private Integer id;
    private Integer postId;
    private String userKey;
    private String platform;
    private LocalDateTime createdAt;

    public ForumShare() {}

    public ForumShare(Integer postId, String userKey, String platform, LocalDateTime createdAt) {
        this.postId = postId;
        this.userKey = userKey;
        this.platform = platform;
        this.createdAt = createdAt;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getPostId() { return postId; }
    public void setPostId(Integer postId) { this.postId = postId; }

    public String getUserKey() { return userKey; }
    public void setUserKey(String userKey) { this.userKey = userKey; }

    public String getPlatform() { return platform; }
    public void setPlatform(String platform) { this.platform = platform; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() {
        return "ForumShare{" +
                "id=" + id +
                ", postId=" + postId +
                ", userKey='" + userKey + '\'' +
                ", platform='" + platform + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}

