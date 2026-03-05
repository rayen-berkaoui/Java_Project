package com.esprit.services;

import com.esprit.entities.ForumComment;
import com.esprit.entities.ForumPost;
import com.esprit.entities.ForumReport;

import java.sql.SQLException;
import java.util.List;

public class ForumAdminService {

    private final ForumPostService postService;
    private final ForumCommentService commentService;
    private final ForumReportService reportService;

    public ForumAdminService() {
        this.postService = new ForumPostService();
        this.commentService = new ForumCommentService();
        this.reportService = new ForumReportService();
    }

    public List<ForumPost> getAllPosts() throws SQLException {
        return postService.afficher();
    }

    public List<ForumComment> getAllComments() throws SQLException {
        return commentService.getAllComments();
    }

    public List<ForumReport> getAllReports() throws SQLException {
        return reportService.getAllReports();
    }

    public List<ForumReport> getPendingReports() throws SQLException {
        return reportService.getPendingReports();
    }

    public void deletePost(int postId) throws SQLException {
        postService.supprimer(postId);
    }

    public void deleteComment(int commentId) throws SQLException {
        commentService.supprimer(commentId);
    }

    public void deletePostWithComments(int postId) throws SQLException {
        postService.supprimer(postId);
    }

    public void resolveReport(int reportId, String action) throws SQLException {
        reportService.updateReportStatus(reportId, action);
    }

    public void resolveReportAndDeleteContent(int reportId) throws SQLException {
        ForumReport targetReport = reportService.getReportById(reportId);

        if (targetReport != null) {
            System.out.println("📋 Processing report #" + reportId + " on " + targetReport.getContentType());

            if ("POST".equals(targetReport.getContentType()) && targetReport.getPostId() != null) {
                postService.supprimer(targetReport.getPostId());
                System.out.println("✅ Deleted post #" + targetReport.getPostId());

            } else if ("COMMENT".equals(targetReport.getContentType()) && targetReport.getCommentId() != null) {
                commentService.supprimer(targetReport.getCommentId());
                System.out.println("✅ Deleted comment #" + targetReport.getCommentId());
            }

            reportService.updateReportStatus(reportId, "RESOLVED");
            System.out.println("✅ ForumReport #" + reportId + " resolved");
        }
    }

    public void banUser(String userKey, String reason) {
        System.out.println("\n🔔 BAN NOTIFICATION (TEMPORARY)");
        System.out.println("   User to ban: " + userKey);
        System.out.println("   Reason: " + reason);
        System.out.println("   Status: This will be implemented when users table is ready");
        System.out.println("   Action: No database changes yet");
    }

}


