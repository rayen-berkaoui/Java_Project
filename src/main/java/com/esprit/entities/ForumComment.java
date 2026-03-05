package com.esprit.entities;

import java.time.LocalDateTime;

public class ForumComment {
        private Integer id;
        private Integer postId;
        private String userKey;
        private String content;
        private int likesCount;
        private int dislikesCount;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public ForumComment() {}

        public ForumComment(Integer postId, String userKey, String content,
                       LocalDateTime createdAt, LocalDateTime updatedAt) {
            this.postId = postId;
            this.userKey = userKey;
            this.content = content;
            this.createdAt = createdAt;
            this.updatedAt = updatedAt;
            this.likesCount = 0;
            this.dislikesCount = 0;
        }

        public Integer getId() { return id; }
        public void setId(Integer id) { this.id = id; }

        public Integer getPostId() { return postId; }
        public void setPostId(Integer postId) { this.postId = postId; }

        public String getUserKey() { return userKey; }
        public void setUserKey(String userKey) { this.userKey = userKey; }

        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }

        public int getLikesCount() { return likesCount; }
        public void setLikesCount(int likesCount) { this.likesCount = likesCount; }

        public int getDislikesCount() { return dislikesCount; }
        public void setDislikesCount(int dislikesCount) { this.dislikesCount = dislikesCount; }

        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

        public LocalDateTime getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

        @Override
        public String toString() {
            return "ForumComment{" +
                    "id=" + id +
                    ", postId=" + postId +
                    ", userKey='" + userKey + '\'' +
                    ", content='" + content + '\'' +
                    ", likesCount=" + likesCount +
                    ", dislikesCount=" + dislikesCount +
                    ", createdAt=" + createdAt +
                    ", updatedAt=" + updatedAt +
                    '}';
        }
    }

