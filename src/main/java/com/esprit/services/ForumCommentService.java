package com.esprit.services;

import com.esprit.entities.ForumComment;
import com.esprit.utils.MyDataBase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ForumCommentService {

    private final Connection con;

    public ForumCommentService() {
        this.con = MyDataBase.getInstance().getConnection();
    }

    public void ajouter(ForumComment c) throws SQLException {
        String sql = "INSERT INTO comments(post_id, user_key, content, created_at, updated_at) VALUES(?, ?, ?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, c.getPostId());
            ps.setString(2, c.getUserKey());
            ps.setString(3, c.getContent());
            ps.setTimestamp(4, Timestamp.valueOf(c.getCreatedAt()));
            ps.setTimestamp(5, c.getUpdatedAt() == null ? null : Timestamp.valueOf(c.getUpdatedAt()));
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) c.setId(rs.getInt(1));
            }
        }
    }

    public void modifier(int id, ForumComment c) throws SQLException {
        String sql = "UPDATE comments SET content = ?, updated_at = ? WHERE id = ?";
        java.sql.Timestamp ts = c.getUpdatedAt() != null ? Timestamp.valueOf(c.getUpdatedAt()) : Timestamp.valueOf(java.time.LocalDateTime.now());
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, c.getContent() != null ? c.getContent() : "");
            ps.setTimestamp(2, ts);
            ps.setInt(3, id);
            ps.executeUpdate();
        }
    }

    public void supprimer(int id) throws SQLException {
        try (PreparedStatement psDel = con.prepareStatement("DELETE FROM activities WHERE comment_id = ?")) {
            psDel.setInt(1, id);
            psDel.executeUpdate();
        } catch (SQLException ignored) {}
        try (PreparedStatement ps = con.prepareStatement("DELETE FROM comments WHERE id = ?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    public int countByPostId(int postId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM comments WHERE post_id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, postId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    public List<ForumComment> afficherByPost(int postId) throws SQLException {
        List<ForumComment> list = new ArrayList<>();
        String sql = "SELECT id, post_id, user_key, content, created_at, updated_at, " +
                "COALESCE(likes_count, 0) AS likes_count, COALESCE(dislikes_count, 0) AS dislikes_count " +
                "FROM comments WHERE post_id = ? ORDER BY created_at ASC";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, postId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ForumComment c = new ForumComment();
                    c.setId(rs.getInt("id"));
                    c.setPostId(rs.getInt("post_id"));
                    c.setUserKey(rs.getString("user_key"));
                    c.setContent(rs.getString("content"));
                    c.setLikesCount(rs.getInt("likes_count"));
                    c.setDislikesCount(rs.getInt("dislikes_count"));
                    Timestamp created = rs.getTimestamp("created_at");
                    Timestamp updated = rs.getTimestamp("updated_at");
                    c.setCreatedAt(created != null ? created.toLocalDateTime() : null);
                    c.setUpdatedAt(updated != null ? updated.toLocalDateTime() : null);
                    list.add(c);
                }
            }
        }
        return list;
    }

    public void syncCommentReactionCounts(int commentId) throws SQLException {
        int[] counts = getCommentCounts(commentId);
        String sql = "UPDATE comments SET likes_count = ?, dislikes_count = ? WHERE id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, counts[0]);
            ps.setInt(2, counts[1]);
            ps.setInt(3, commentId);
            ps.executeUpdate();
        }
    }

    public void syncAllCommentReactionCounts() throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("SELECT id FROM comments");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) syncCommentReactionCounts(rs.getInt("id"));
        }
    }

    public int[] getCommentCounts(int commentId) throws SQLException {
        String sql = "SELECT " +
                "(SELECT COUNT(*) FROM activities WHERE comment_id=? AND activity_type='LIKE') AS likes_count, " +
                "(SELECT COUNT(*) FROM activities WHERE comment_id=? AND activity_type='DISLIKE') AS dislikes_count";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, commentId);
            ps.setInt(2, commentId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new int[]{rs.getInt("likes_count"), rs.getInt("dislikes_count")};
                }
                return new int[]{0, 0};
            }
        }
    }

    public List<ForumComment> getAllComments() throws SQLException {
        List<ForumComment> list = new ArrayList<>();
        String sql = "SELECT id, post_id, user_key, content, created_at, updated_at, " +
                "COALESCE(likes_count, 0) AS likes_count, COALESCE(dislikes_count, 0) AS dislikes_count " +
                "FROM comments ORDER BY created_at DESC";

        try (PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                ForumComment c = new ForumComment();
                c.setId(rs.getInt("id"));
                c.setPostId(rs.getInt("post_id"));
                c.setUserKey(rs.getString("user_key"));
                c.setContent(rs.getString("content"));
                c.setLikesCount(rs.getInt("likes_count"));
                c.setDislikesCount(rs.getInt("dislikes_count"));
                Timestamp created = rs.getTimestamp("created_at");
                Timestamp updated = rs.getTimestamp("updated_at");
                c.setCreatedAt(created != null ? created.toLocalDateTime() : null);
                c.setUpdatedAt(updated != null ? updated.toLocalDateTime() : null);
                list.add(c);
            }
        }
        return list;
    }
}


