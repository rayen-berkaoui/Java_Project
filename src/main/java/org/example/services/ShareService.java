package org.example.services;

import org.example.entities.Share;
import org.example.utils.Db;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ShareService {

    private final Connection con;

    public ShareService() {
        this.con = Db.getInstance().getConnection();
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

    public List<Share> getSharesByPost(int postId) throws SQLException {
        List<Share> shares = new ArrayList<>();
        String sql = "SELECT * FROM shares WHERE post_id=? ORDER BY created_at DESC";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, postId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Share share = new Share();
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

    public List<Share> getSharesByUser(String userKey) throws SQLException {
        List<Share> shares = new ArrayList<>();
        String sql = "SELECT * FROM shares WHERE user_key=? ORDER BY created_at DESC";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, userKey);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Share share = new Share();
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
