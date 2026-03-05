package com.esprit.services;

import com.esprit.entities.ForumShare;
import com.esprit.utils.MyDataBase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ForumShareService {

    private final Connection con;

    public ForumShareService() {
        this.con = MyDataBase.getInstance().getConnection();
    }

    public void addShare(int postId, String userKey, String platform) throws SQLException {
        String sql = "INSERT INTO shares(post_id, user_key, platform, created_at) VALUES(?,?,?, NOW())";
        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, postId);
            ps.setString(2, userKey);
            ps.setString(3, platform);
            ps.executeUpdate();
        }
    }

    public int countShares(int postId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM shares WHERE post_id=?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, postId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    public List<ForumShare> getSharesByPost(int postId) throws SQLException {
        List<ForumShare> shares = new ArrayList<>();
        String sql = "SELECT * FROM shares WHERE post_id=? ORDER BY created_at DESC";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, postId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ForumShare share = new ForumShare();
                    share.setId(rs.getInt("id"));
                    share.setPostId(rs.getInt("post_id"));
                    share.setUserKey(rs.getString("user_key"));
                    share.setPlatform(rs.getString("platform"));
                    share.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                    shares.add(share);
                }
            }
        }
        return shares;
    }

    public List<ForumShare> getSharesByUser(String userKey) throws SQLException {
        List<ForumShare> shares = new ArrayList<>();
        String sql = "SELECT * FROM shares WHERE user_key=? ORDER BY created_at DESC";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, userKey);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ForumShare share = new ForumShare();
                    share.setId(rs.getInt("id"));
                    share.setPostId(rs.getInt("post_id"));
                    share.setUserKey(rs.getString("user_key"));
                    share.setPlatform(rs.getString("platform"));
                    share.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                    shares.add(share);
                }
            }
        }
        return shares;
    }
}


