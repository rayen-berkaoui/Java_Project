package org.example.services;

import org.example.entities.Report;
import org.example.utils.Db;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ReportService {

    private final Connection con;

    public ReportService() {
        this.con = Db.getInstance().getConnection();
    }

    public void addReport(Report report) throws SQLException {
        String sql = "INSERT INTO reports (post_id, comment_id, reporter_user_key, reported_user_key, reason, content_type, status) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setObject(1, report.getPostId());
            ps.setObject(2, report.getCommentId());
            ps.setString(3, report.getReporterUserKey());
            ps.setString(4, report.getReportedUserKey());
            ps.setString(5, report.getReason());
            ps.setString(6, report.getContentType());
            ps.setString(7, report.getStatus() != null ? report.getStatus() : "PENDING");

            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    report.setId(rs.getInt(1));
                }
            }
        }
    }

    public List<Report> getAllReports() throws SQLException {
        List<Report> reports = new ArrayList<>();
        String sql = "SELECT * FROM reports ORDER BY created_at DESC";

        try (Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                reports.add(mapReport(rs));
            }
        }
        return reports;
    }

    public List<Report> getPendingReports() throws SQLException {
        List<Report> reports = new ArrayList<>();
        String sql = "SELECT * FROM reports WHERE status = 'PENDING' ORDER BY created_at DESC";

        try (PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                reports.add(mapReport(rs));
            }
        }
        return reports;
    }

    public void updateReportStatus(int reportId, String status) throws SQLException {
        String sql = "UPDATE reports SET status = ? WHERE id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, reportId);
            ps.executeUpdate();
        }
    }

    public void deleteReport(int reportId) throws SQLException {
        String sql = "DELETE FROM reports WHERE id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, reportId);
            ps.executeUpdate();
        }
    }

    private Report mapReport(ResultSet rs) throws SQLException {
        Report r = new Report();
        r.setId(rs.getInt("id"));
        r.setPostId(rs.getObject("post_id") != null ? rs.getInt("post_id") : null);
        r.setCommentId(rs.getObject("comment_id") != null ? rs.getInt("comment_id") : null);
        r.setReporterUserKey(rs.getString("reporter_user_key"));
        r.setReportedUserKey(rs.getString("reported_user_key"));
        r.setReason(rs.getString("reason"));
        r.setContentType(rs.getString("content_type"));
        r.setStatus(rs.getString("status"));
        r.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        return r;
    }

    public Report getReportById(int reportId) throws SQLException {
        String sql = "SELECT * FROM reports WHERE id = ?";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, reportId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapReport(rs);
                }
            }
        }
        return null;
    }
}
