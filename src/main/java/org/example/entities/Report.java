package org.example.entities;

import java.time.LocalDateTime;

public class Report {
    private Integer id;
    private Integer postId;
    private Integer commentId;
    private String reporterUserKey;
    private String reportedUserKey;
    private String reason;
    private String contentType;
    private LocalDateTime createdAt;
    private String status;

    public Report() {}

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getPostId() { return postId; }
    public void setPostId(Integer postId) { this.postId = postId; }

    public Integer getCommentId() { return commentId; }
    public void setCommentId(Integer commentId) { this.commentId = commentId; }

    public String getReporterUserKey() { return reporterUserKey; }
    public void setReporterUserKey(String reporterUserKey) { this.reporterUserKey = reporterUserKey; }

    public String getReportedUserKey() { return reportedUserKey; }
    public void setReportedUserKey(String reportedUserKey) { this.reportedUserKey = reportedUserKey; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    @Override
    public String toString() {
        return "Report{" + "id=" + id + ", postId=" + postId + ", commentId=" + commentId + ", reporterUserKey='" + reporterUserKey + '\'' + ", reportedUserKey='" + reportedUserKey + '\'' + ", reason='" + reason + '\'' + ", contentType='" + contentType + '\'' + ", createdAt=" + createdAt + ", status='" + status + '\'' + '}';
    }
}
