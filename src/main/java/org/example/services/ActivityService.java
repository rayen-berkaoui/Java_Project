package org.example.services;

import org.example.entities.Activity;
import org.example.entities.ReactionType;
import org.example.utils.Db;
import org.example.services.ReportService;
import org.example.entities.Report;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ActivityService {

    private final Connection con;

    public ActivityService() {
        this.con = Db.getInstance().getConnection();
    }

    public String toggleReaction(int postId, String userKey, ReactionType type) throws SQLException {
        String checkSql = "SELECT id, activity_type FROM activities WHERE post_id=? AND user_key=? AND activity_type IN ('LIKE', 'DISLIKE')";

        try (PreparedStatement psCheck = con.prepareStatement(checkSql)) {
            psCheck.setInt(1, postId);
            psCheck.setString(2, userKey);

            try (ResultSet rs = psCheck.executeQuery()) {
                if (rs.next()) {
                    int id = rs.getInt("id");
                    String existingType = rs.getString("activity_type");

                    if (existingType.equals(type.name())) {
                        String deleteSql = "DELETE FROM activities WHERE id=?";
                        try (PreparedStatement psDelete = con.prepareStatement(deleteSql)) {
                            psDelete.setInt(1, id);
                            psDelete.executeUpdate();
                        }
                        return "REMOVED";
                    } else {
                        String updateSql = "UPDATE activities SET activity_type=?, created_at=NOW() WHERE id=?";
                        try (PreparedStatement psUpdate = con.prepareStatement(updateSql)) {
                            psUpdate.setString(1, type.name());
                            psUpdate.setInt(2, id);
                            psUpdate.executeUpdate();
                        }
                        return "SWITCHED";
                    }
                } else {
                    String insertSql = "INSERT INTO activities(post_id, user_key, activity_type, created_at) VALUES(?,?,?, NOW())";
                    try (PreparedStatement psInsert = con.prepareStatement(insertSql)) {
                        psInsert.setInt(1, postId);
                        psInsert.setString(2, userKey);
                        psInsert.setString(3, type.name());
                        psInsert.executeUpdate();
                    }
                    return "INSERTED";
                }
            }
        }
    }

    public int countLikes(int postId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM activities WHERE post_id=? AND activity_type='LIKE'";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, postId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    public int countDislikes(int postId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM activities WHERE post_id=? AND activity_type='DISLIKE'";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, postId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    private final CommentService commentService = new CommentService();

    public void toggleCommentReaction(int commentId, int postId, String userKey, ReactionType type) throws SQLException {

        String checkSql = "SELECT id, activity_type FROM activities WHERE comment_id=? AND user_key=? AND activity_type IN ('LIKE', 'DISLIKE')";
        try (PreparedStatement psCheck = con.prepareStatement(checkSql)) {
            psCheck.setInt(1, commentId);
            psCheck.setString(2, userKey);
            try (ResultSet rs = psCheck.executeQuery()) {
                if (rs.next()) {
                    int id = rs.getInt("id");
                    String existingType = rs.getString("activity_type");
                    if (existingType.equals(type.name())) {

                        try (PreparedStatement psDelete = con.prepareStatement("DELETE FROM activities WHERE id=?")) {
                            psDelete.setInt(1, id);
                            psDelete.executeUpdate();
                        }
                    } else {

                        try (PreparedStatement psUpdate = con.prepareStatement("UPDATE activities SET activity_type=?, created_at=NOW() WHERE id=?")) {
                            psUpdate.setString(1, type.name());
                            psUpdate.setInt(2, id);
                            psUpdate.executeUpdate();
                        }
                    }
                } else {

                    String checkPost = "SELECT id FROM activities WHERE post_id=? AND user_key=? AND activity_type=? AND comment_id IS NULL";
                    boolean conflictExists = false;
                    try (PreparedStatement psCP = con.prepareStatement(checkPost)) {
                        psCP.setInt(1, postId);
                        psCP.setString(2, userKey);
                        psCP.setString(3, type.name());
                        try (ResultSet rsCP = psCP.executeQuery()) {
                            conflictExists = rsCP.next();
                        }
                    }

                    try {
                        String insertSql = "INSERT INTO activities(post_id, comment_id, user_key, activity_type, created_at) VALUES(?,?,?,?, NOW())";
                        try (PreparedStatement psInsert = con.prepareStatement(insertSql)) {
                            psInsert.setInt(1, postId);
                            psInsert.setInt(2, commentId);
                            psInsert.setString(3, userKey);
                            psInsert.setString(4, type.name());
                            psInsert.executeUpdate();
                        }
                    } catch (SQLException insertEx) {
                        if (insertEx.getMessage() != null && insertEx.getMessage().contains("Duplicate entry")) {

                            String delSql = "DELETE FROM activities WHERE post_id=? AND user_key=? AND activity_type=?";
                            try (PreparedStatement psDel = con.prepareStatement(delSql)) {
                                psDel.setInt(1, postId);
                                psDel.setString(2, userKey);
                                psDel.setString(3, type.name());
                                psDel.executeUpdate();
                            }

                            String insertSql2 = "INSERT INTO activities(post_id, comment_id, user_key, activity_type, created_at) VALUES(?,?,?,?, NOW())";
                            try (PreparedStatement psRetry = con.prepareStatement(insertSql2)) {
                                psRetry.setInt(1, postId);
                                psRetry.setInt(2, commentId);
                                psRetry.setString(3, userKey);
                                psRetry.setString(4, type.name());
                                psRetry.executeUpdate();
                            }
                        } else {
                            throw insertEx;
                        }
                    }
                }
                commentService.syncCommentReactionCounts(commentId);
            }
        }
    }

    public int countCommentLikes(int commentId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM activities WHERE comment_id=? AND activity_type='LIKE'";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, commentId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    public int countCommentDislikes(int commentId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM activities WHERE comment_id=? AND activity_type='DISLIKE'";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, commentId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    public void addShare(int postId, String userKey, String platform) throws SQLException {
        String sql = "INSERT INTO activities(post_id, user_key, activity_type, platform, created_at) VALUES(?,?, 'SHARE', ?, NOW())";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, postId);
            ps.setString(2, userKey);
            ps.setString(3, platform);
            ps.executeUpdate();
        }
    }

    public int countShares(int postId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM activities WHERE post_id=? AND activity_type='SHARE'";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, postId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    public String reportPost(int postId, String userKey, String reason) throws SQLException {
        String activitySql = "INSERT INTO activities(post_id, user_key, activity_type, reason, created_at) VALUES(?,?, 'REPORT', ?, NOW())";
        try {
            try (PreparedStatement ps = con.prepareStatement(activitySql)) {
                ps.setInt(1, postId);
                ps.setString(2, userKey);
                ps.setString(3, reason);
                ps.executeUpdate();}

            String getUserSql = "SELECT user_key FROM posts WHERE id = ?";
            String reportedUserKey = null;

            try (PreparedStatement ps = con.prepareStatement(getUserSql)) {
                ps.setInt(1, postId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        reportedUserKey = rs.getString("user_key");
                    }
                }
            }

            if (reportedUserKey != null) {
                Report report = new Report();
                report.setPostId(postId);
                report.setCommentId(null);
                report.setReporterUserKey(userKey);
                report.setReportedUserKey(reportedUserKey);
                report.setReason(reason);
                report.setContentType("POST");
                report.setStatus("PENDING");

                ReportService reportService = new ReportService();
                reportService.addReport(report);

                System.out.println("✅ Report saved to reports table with ID: " + report.getId());
            }

            return "REPORTED";

        } catch (SQLIntegrityConstraintViolationException e) {
            return "ALREADY_REPORTED";
        }
    }

    public int countReports(int postId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM activities WHERE post_id=? AND activity_type='REPORT'";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, postId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    public List<Activity> getActivitiesByPost(int postId) throws SQLException {
        List<Activity> list = new ArrayList<>();
        String sql = "SELECT * FROM activities WHERE post_id = ? ORDER BY created_at DESC";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, postId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Activity a = new Activity();
                    a.setId(rs.getInt("id"));
                    a.setPostId(rs.getInt("post_id"));
                    a.setUserKey(rs.getString("user_key"));
                    a.setActivityType(rs.getString("activity_type"));
                    a.setPlatform(rs.getString("platform"));
                    a.setReason(rs.getString("reason"));
                    a.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                    list.add(a);
                }
            }
        }
        return list;
    }
}
