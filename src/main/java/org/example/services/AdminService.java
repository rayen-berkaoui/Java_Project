package org.example.services;

import org.example.entities.Comment;
import org.example.entities.Post;
import org.example.entities.Report;

import java.sql.SQLException;
import java.util.List;

public class AdminService {

    private final PostService postService;
    private final CommentService commentService;
    private final ReportService reportService;

    public AdminService() {
        this.postService = new PostService();
        this.commentService = new CommentService();
        this.reportService = new ReportService();
    }

    public List<Post> getAllPosts() throws SQLException {
        return postService.afficher();
    }

    public List<Comment> getAllComments() throws SQLException {
        return commentService.getAllComments();
    }

    public List<Report> getAllReports() throws SQLException {
        return reportService.getAllReports();
    }

    public List<Report> getPendingReports() throws SQLException {
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
        Report targetReport = reportService.getReportById(reportId);

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
            System.out.println("✅ Report #" + reportId + " resolved");
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
