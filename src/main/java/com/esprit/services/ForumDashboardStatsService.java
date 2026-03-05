package com.esprit.services;

import com.esprit.utils.MyDataBase;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

public class ForumDashboardStatsService {

    private final Connection con;

    public ForumDashboardStatsService() {
        this.con = MyDataBase.getInstance().getConnection();
    }

    public int countPosts() {
        return countTable("posts");
    }

    public int countComments() {
        return countTable("comments");
    }

    public int countShares() {
        return countTable("shares");
    }

    public int countReports() {
        return countTable("reports");
    }

    public int countWarnings() {
        return countTable("user_warnings");
    }

    public int countBlockedUsers() {
        try (PreparedStatement ps = con.prepareStatement(
                "SELECT COUNT(*) FROM user_warnings WHERE blocked_until IS NOT NULL AND blocked_until > NOW()")) {
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("[DashboardStats] countBlockedUsers: " + e.getMessage());
        }
        return 0;
    }

    private int countTable(String table) {
        try (Statement st = con.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM " + table)) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            System.err.println("[DashboardStats] count " + table + ": " + e.getMessage());
        }
        return 0;
    }

    public Map<String, Integer> getPostsPerDay(int days) {
        return getCountPerDay("posts", "created_at", days);
    }

    public Map<String, Integer> getCommentsPerDay(int days) {
        return getCountPerDay("comments", "created_at", days);
    }

    private Map<String, Integer> getCountPerDay(String table, String dateCol, int days) {
        Map<String, Integer> map = new LinkedHashMap<>();

        LocalDate today = LocalDate.now();
        for (int i = days - 1; i >= 0; i--) {
            map.put(today.minusDays(i).toString(), 0);
        }
        String sql = "SELECT DATE(" + dateCol + ") AS d, COUNT(*) AS c FROM " + table +
                " WHERE " + dateCol + " >= ? GROUP BY DATE(" + dateCol + ") ORDER BY d";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(today.minusDays(days).atStartOfDay()));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String date = rs.getString("d");
                    int count = rs.getInt("c");
                    if (map.containsKey(date)) map.put(date, count);
                }
            }
        } catch (SQLException e) {
            System.err.println("[DashboardStats] getCountPerDay(" + table + "): " + e.getMessage());
        }
        return map;
    }

    public Map<String, Integer> getReactionCounts() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("LIKE", 0);
        map.put("DISLIKE", 0);
        map.put("SHARE", 0);
        String sql = "SELECT activity_type, COUNT(*) as c FROM activities GROUP BY activity_type";
        try (Statement st = con.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                map.put(rs.getString("activity_type"), rs.getInt("c"));
            }
        } catch (SQLException e) {
            System.err.println("[DashboardStats] getReactionCounts: " + e.getMessage());
        }
        return map;
    }

    public Map<String, Integer> getTopHashtags(int limit) {
        Map<String, Integer> allTags = new LinkedHashMap<>();
        String sql = "SELECT content FROM posts WHERE content IS NOT NULL";
        try (Statement st = con.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                String content = rs.getString("content");

                int idx = content.indexOf("[HASHTAGS:");
                if (idx >= 0) {
                    int end = content.indexOf("]", idx);
                    if (end > idx) {
                        String tags = content.substring(idx + 10, end);
                        for (String t : tags.split(",")) {
                            String tag = t.trim().toLowerCase();
                            if (!tag.isEmpty()) allTags.merge(tag, 1, Integer::sum);
                        }
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("[DashboardStats] getTopHashtags: " + e.getMessage());
        }

        return allTags.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(limit)
                .collect(LinkedHashMap::new, (m, e) -> m.put(e.getKey(), e.getValue()), Map::putAll);
    }

    public Map<String, Integer> getTopCommenters(int limit) {
        Map<String, Integer> map = new LinkedHashMap<>();
        String sql = "SELECT user_key, COUNT(*) AS c FROM comments GROUP BY user_key ORDER BY c DESC LIMIT ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    map.put(rs.getString("user_key"), rs.getInt("c"));
                }
            }
        } catch (SQLException e) {
            System.err.println("[DashboardStats] getTopCommenters: " + e.getMessage());
        }
        return map;
    }

    public Map<String, Integer> getReportsByStatus() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("PENDING", 0);
        map.put("RESOLVED", 0);
        map.put("DISMISSED", 0);
        String sql = "SELECT status, COUNT(*) AS c FROM reports GROUP BY status";
        try (Statement st = con.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                map.put(rs.getString("status"), rs.getInt("c"));
            }
        } catch (SQLException e) {
            System.err.println("[DashboardStats] getReportsByStatus: " + e.getMessage());
        }
        return map;
    }

    public Map<String, Integer> getWarningsByLevel() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("Alerte 1", 0);
        map.put("Alerte 2", 0);
        map.put("Bloqué", 0);
        String sql = "SELECT action_taken, COUNT(*) AS c FROM warning_history GROUP BY action_taken";
        try (Statement st = con.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                String action = rs.getString("action_taken");
                int count = rs.getInt("c");
                switch (action) {
                    case "WARNING_1" -> map.put("Alerte 1", count);
                    case "WARNING_2" -> map.put("Alerte 2", count);
                    case "BLOCKED" -> map.put("Bloqué", count);
                }
            }
        } catch (SQLException e) {
            System.err.println("[DashboardStats] getWarningsByLevel: " + e.getMessage());
        }
        return map;
    }

    public Map<String, Integer> getPostsByHour() {
        Map<String, Integer> map = new LinkedHashMap<>();
        for (int h = 0; h < 24; h++) {
            map.put(String.format("%02d:00", h), 0);
        }
        String sql = "SELECT HOUR(created_at) AS h, COUNT(*) AS c FROM posts GROUP BY HOUR(created_at)";
        try (Statement st = con.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                int hour = rs.getInt("h");
                map.put(String.format("%02d:00", hour), rs.getInt("c"));
            }
        } catch (SQLException e) {
            System.err.println("[DashboardStats] getPostsByHour: " + e.getMessage());
        }
        return map;
    }

    public Map<String, Integer> getTopPostsByComments(int limit) {
        Map<String, Integer> map = new LinkedHashMap<>();
        String sql = """
            SELECT p.id, SUBSTRING(COALESCE(p.content,''), 1, 30) AS title, COUNT(c.id) AS cnt
            FROM posts p LEFT JOIN comments c ON c.post_id = p.id
            GROUP BY p.id, title
            ORDER BY cnt DESC
            LIMIT ?
        """;
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String label = "ForumPost #" + rs.getInt("id");
                    String title = rs.getString("title");
                    if (title != null && !title.isBlank()) label = title.length() > 25 ? title.substring(0, 22) + "..." : title;
                    map.put(label, rs.getInt("cnt"));
                }
            }
        } catch (SQLException e) {
            System.err.println("[DashboardStats] getTopPostsByComments: " + e.getMessage());
        }
        return map;
    }
}


