package com.esprit.services;

import com.esprit.entities.ForumPost;
import com.esprit.utils.MyDataBase;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ForumPostService implements ForumCRUD<ForumPost> {
    Connection con;

    public ForumPostService() {
        con = MyDataBase.getInstance().getConnection();
    }

    @Override
    public void ajouter(ForumPost post) throws SQLException {
        String contentEncoded = ForumPost.encodeForDb(post.getContent(), post.getHashtags());
        String sql = "INSERT INTO posts(content, image_path, video_path, created_at, updated_at) VALUES(?, ?, ?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, contentEncoded);
            ps.setString(2, post.getImagePath());
            ps.setString(3, post.getVideoPath());
            ps.setTimestamp(4, Timestamp.valueOf(post.getCreatedAt()));
            ps.setTimestamp(5, post.getUpdatedAt() == null ? null : Timestamp.valueOf(post.getUpdatedAt()));
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) post.setId(rs.getInt(1));
            }
        }
    }

    @Override
    public void modifie(int id, ForumPost post) throws SQLException {
        String contentEncoded = ForumPost.encodeForDb(post.getContent(), post.getHashtags());
        java.time.LocalDateTime now = post.getUpdatedAt() != null ? post.getUpdatedAt() : java.time.LocalDateTime.now();
        String sql = "UPDATE posts SET content = ?, image_path = ?, video_path = ?, updated_at = ? WHERE id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, contentEncoded);
            ps.setString(2, post.getImagePath());
            ps.setString(3, post.getVideoPath());
            ps.setTimestamp(4, Timestamp.valueOf(now));
            ps.setInt(5, id);
            ps.executeUpdate();
        }
    }

    @Override
    public void supprimer(int id) throws SQLException {
        deleteIfExists("DELETE FROM comments WHERE post_id = ?", id);
        deleteIfExists("DELETE FROM activities WHERE post_id = ?", id);
        deleteIfExists("DELETE FROM shares WHERE post_id = ?", id);
        deleteIfExists("DELETE FROM reports WHERE post_id = ?", id);

        try (PreparedStatement ps = con.prepareStatement("DELETE FROM posts WHERE id = ?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    private void deleteIfExists(String sql, int postId) {
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, postId);
            ps.executeUpdate();
        } catch (SQLException ignored) {
        }
    }

    @Override
    public List<ForumPost> afficher() throws SQLException {
        if (con == null) {
            System.out.println("Warning: DB connection is null — returning empty posts list");
            return new ArrayList<>();
        }

        List<ForumPost> posts = new ArrayList<>();
        String sql = "SELECT * FROM posts ORDER BY created_at DESC";
        try (Statement statement = con.createStatement(); ResultSet rs = statement.executeQuery(sql)) {
            while (rs.next()) {
                posts.add(mapPost(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw e;
        }

        return posts;
    }

    public ForumPost getById(int id) throws SQLException {
        if (con == null) return null;
        String sql = "SELECT * FROM posts WHERE id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapPost(rs);
            }
        }
        return null;
    }

    public List<ForumPost> getPostsBetweenDates(LocalDateTime start, LocalDateTime end) throws SQLException {
        if (con == null) {
            System.out.println("Warning: DB connection is null — getPostsBetweenDates returns empty list");
            return new ArrayList<>();
        }

        String sql = "SELECT * FROM posts WHERE created_at BETWEEN ? AND ? ORDER BY created_at DESC";
        PreparedStatement ps = con.prepareStatement(sql);
        ps.setTimestamp(1, Timestamp.valueOf(start));
        ps.setTimestamp(2, Timestamp.valueOf(end));

        try (ResultSet rs = ps.executeQuery()) {
            List<ForumPost> posts = new ArrayList<>();
            while (rs.next()) posts.add(mapPost(rs));
            return posts;
        }
    }

    public void displayPostsBetweenDates(LocalDateTime start, LocalDateTime end) {
        try {
            List<ForumPost> posts = getPostsBetweenDates(start, end);

            if (posts.isEmpty()) {
                System.out.println("No posts found.");
            } else {
                System.out.println("\n=== Posts Last 7 Days ===");
                for (ForumPost p : posts) {
                    System.out.println("----------------------------");
                    System.out.println("ForumPost ID: " + p.getId());
                    System.out.println("Content: " + p.getContent());
                    System.out.println("Created At: " + p.getCreatedAt());
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<ForumPost> searchPosts(String word) throws SQLException {
        if (con == null) {
            System.out.println("Warning: DB connection is null — searchPosts returns empty list");
            return new ArrayList<>();
        }

        String pattern = "%" + word.toLowerCase() + "%";
        String sql = "SELECT * FROM posts WHERE LOWER(COALESCE(content,'')) LIKE ? ORDER BY created_at DESC";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, pattern);
            try (ResultSet rs = ps.executeQuery()) {
                List<ForumPost> list = new ArrayList<>();
                while (rs.next()) list.add(mapPost(rs));
                return list;
            }
        }
    }

    public List<ForumPost> filterPosts(String searchText, String hashtag, LocalDateTime dateFrom, LocalDateTime dateTo) {
        List<String> tagList = parseHashtagFilter(hashtag);
        return filterPosts(searchText, tagList, dateFrom, dateTo);
    }

    private static List<String> parseHashtagFilter(String hashtag) {
        List<String> out = new ArrayList<>();
        if (hashtag == null || hashtag.isBlank()) return out;
        for (String s : hashtag.split("[,\\s]+")) {
            String t = s.trim().replace("#", "").toLowerCase();
            if (!t.isEmpty()) out.add(t);
        }
        return out;
    }

    public List<ForumPost> filterPosts(String searchText, List<String> hashtags, LocalDateTime dateFrom, LocalDateTime dateTo) {
        if (con == null) return new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT * FROM posts WHERE 1=1");
        List<Object> params = new ArrayList<>();
        if (searchText != null && !searchText.isBlank()) {
            sql.append(" AND LOWER(COALESCE(content,'')) LIKE ?");
            params.add("%" + searchText.toLowerCase().trim() + "%");
        }
        if (hashtags != null && !hashtags.isEmpty()) {
            sql.append(" AND (");
            for (int i = 0; i < hashtags.size(); i++) {
                if (i > 0) sql.append(" OR ");
                sql.append("(LOWER(COALESCE(content,'')) LIKE ? OR LOWER(COALESCE(content,'')) LIKE ?)");
                String tag = hashtags.get(i);
                params.add("%" + tag + "%");
                params.add("%#" + tag + "%");
            }
            sql.append(")");
        }
        if (dateFrom != null) {
            sql.append(" AND created_at >= ?");
            params.add(Timestamp.valueOf(dateFrom));
        }
        if (dateTo != null) {
            sql.append(" AND created_at <= ?");
            params.add(Timestamp.valueOf(dateTo));
        }
        sql.append(" ORDER BY created_at DESC");
        try (PreparedStatement ps = con.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                Object o = params.get(i);
                if (o instanceof String) ps.setString(i + 1, (String) o);
                else if (o instanceof Timestamp) ps.setTimestamp(i + 1, (Timestamp) o);
            }
            try (ResultSet rs = ps.executeQuery()) {
                List<ForumPost> list = new ArrayList<>();
                while (rs.next()) list.add(mapPost(rs));
                return list;
            }
        } catch (SQLException e) {
            return filterPostsSimple(searchText, hashtags, dateFrom, dateTo);
        }
    }

    private List<ForumPost> filterPostsSimple(String searchText, List<String> hashtags, LocalDateTime dateFrom, LocalDateTime dateTo) {
        try {
            List<ForumPost> list = searchText != null && !searchText.isBlank() ? searchPosts(searchText) : afficher();
            if (hashtags != null && !hashtags.isEmpty()) {
                list = list.stream().filter(p -> {
                    for (String postTag : p.getHashtagList()) {
                        for (String filterTag : hashtags)
                            if (postTag.equalsIgnoreCase(filterTag)) return true;
                    }
                    return false;
                }).collect(java.util.stream.Collectors.toList());
            }
            if (dateFrom != null || dateTo != null) {
                LocalDateTime from = dateFrom != null ? dateFrom : LocalDateTime.of(2000, 1, 1, 0, 0);
                LocalDateTime to = dateTo != null ? dateTo : LocalDateTime.now().plusYears(10);
                list = list.stream()
                        .filter(p -> p.getCreatedAt() != null && !p.getCreatedAt().isBefore(from) && !p.getCreatedAt().isAfter(to))
                        .collect(java.util.stream.Collectors.toList());
            }
            return list;
        } catch (SQLException e) {
            return new ArrayList<>();
        }
    }

    public List<String> getDistinctHashtags() {
        java.util.Set<String> set = new java.util.LinkedHashSet<>();
        try {
            if (con != null) {
                for (ForumPost p : afficher()) {
                    for (String t : p.getHashtagList()) set.add(t);
                }
            }
            java.nio.file.Path recPath = java.nio.file.Paths.get(
                    System.getProperty("user.dir"), "config", "recommended_hashtags.txt");
            if (java.nio.file.Files.exists(recPath)) {
                set.addAll(java.nio.file.Files.readAllLines(recPath));
            }
        } catch (Exception ignored) {}
        return new ArrayList<>(set);
    }

    private ForumPost mapPost(ResultSet rs) throws SQLException {
        ForumPost p = new ForumPost();
        p.setId(rs.getInt("id"));
        String raw = rs.getString("content");
        String[] decoded = ForumPost.decodeFromDb(raw);
        p.setContent(decoded[0]);
        p.setHashtags(decoded[1]);
        p.setImagePath(rs.getString("image_path"));
        p.setVideoPath(rs.getString("video_path"));
        p.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        p.setUpdatedAt(rs.getTimestamp("updated_at") != null ? rs.getTimestamp("updated_at").toLocalDateTime() : null);
        return p;
    }

    public boolean isConnected() {
        return this.con != null;
    }

    public java.util.List<java.util.Map.Entry<String, Integer>> getHashtagsWithCount() throws SQLException {
        java.util.Map<String, Integer> map = new java.util.LinkedHashMap<>();
        for (ForumPost p : afficher()) {
            for (String tag : p.getHashtagList()) {
                map.merge(tag, 1, Integer::sum);
            }
        }
        try {
            java.nio.file.Path recPath = java.nio.file.Paths.get(
                    System.getProperty("user.dir"), "config", "recommended_hashtags.txt");
            if (java.nio.file.Files.exists(recPath)) {
                for (String tag : java.nio.file.Files.readAllLines(recPath, java.nio.charset.StandardCharsets.UTF_8)) {
                    tag = tag.trim();
                    if (!tag.isEmpty()) map.putIfAbsent(tag, 0);
                }
            }
        } catch (Exception ignored) {}
        return new java.util.ArrayList<>(map.entrySet());
    }

    public void removeRecommendedHashtag(String tag) throws java.io.IOException {
        if (tag == null || tag.isBlank()) return;
        final String tagClean = tag.trim().replace("#", "");
        java.nio.file.Path path = java.nio.file.Paths.get(
                System.getProperty("user.dir"), "config", "recommended_hashtags.txt");
        if (!java.nio.file.Files.exists(path)) return;
        java.util.List<String> lines = java.nio.file.Files.readAllLines(path, java.nio.charset.StandardCharsets.UTF_8);
        lines.removeIf(l -> l.trim().equalsIgnoreCase(tagClean));
        java.nio.file.Files.write(path, lines, java.nio.charset.StandardCharsets.UTF_8);
    }

    public void removeHashtagFromAllPosts(String tag) throws SQLException {
        if (tag == null || tag.isBlank()) return;
        String tagLower = tag.trim().toLowerCase();
        for (ForumPost p : afficher()) {
            List<String> tags = p.getHashtagList();
            if (tags.stream().anyMatch(t -> t.equalsIgnoreCase(tagLower))) {
                String newTags = tags.stream()
                        .filter(t -> !t.equalsIgnoreCase(tagLower))
                        .reduce((a, b) -> a + "," + b).orElse(null);
                p.setHashtags(newTags);
                p.setUpdatedAt(java.time.LocalDateTime.now());
                modifie(p.getId(), p);
            }
        }
    }

    public void addRecommendedHashtag(String tag) throws java.io.IOException {
        if (tag == null || tag.isBlank()) return;
        tag = tag.trim().replace("#", "");
        if (tag.isEmpty()) return;
        java.nio.file.Path path = java.nio.file.Paths.get(
                System.getProperty("user.dir"), "config", "recommended_hashtags.txt");
        java.nio.file.Files.createDirectories(path.getParent());
        java.util.Set<String> set = new java.util.LinkedHashSet<>();
        if (java.nio.file.Files.exists(path)) {
            set.addAll(java.nio.file.Files.readAllLines(path, java.nio.charset.StandardCharsets.UTF_8));
        }
        set.add(tag);
        java.nio.file.Files.write(path, set, java.nio.charset.StandardCharsets.UTF_8);
    }

}


