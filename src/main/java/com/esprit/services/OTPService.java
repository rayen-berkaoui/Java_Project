package com.esprit.services;

import com.esprit.utils.MyDataBase;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.Random;

public class OTPService {

    private Connection con;
    private static final int OTP_EXPIRY_SECONDS = 60;

    public OTPService() {
        con = MyDataBase.getInstance().getConnection();
    }

    /**
     * Generate a 6-digit OTP code
     */
    public String generateOTP() {
        Random random = new Random();
        int otp = 100000 + random.nextInt(900000); // 100000–999999
        return String.valueOf(otp);
    }

    /**
     * Store OTP in database. Invalidate any previous unused OTPs for this email.
     */
    public boolean storeOTP(String email, String code) {
        try {
            // Invalidate old OTPs for this email
            String invalidateSql = "UPDATE otp_codes SET used = TRUE WHERE email = ? AND used = FALSE";
            PreparedStatement invalidatePs = con.prepareStatement(invalidateSql);
            invalidatePs.setString(1, email);
            invalidatePs.executeUpdate();

            // Insert new OTP
            String sql = "INSERT INTO otp_codes (email, code, created_at, used) VALUES (?, ?, NOW(), FALSE)";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1, email);
            ps.setString(2, code);
            ps.executeUpdate();

            System.out.println("✅ OTP stored for: " + email);
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Verify OTP: check if code matches, is not used, and is within 60 seconds
     */
    public boolean verifyOTP(String email, String code) {
        String sql = "SELECT created_at FROM otp_codes " +
                     "WHERE email = ? AND code = ? AND used = FALSE " +
                     "ORDER BY created_at DESC LIMIT 1";
        try {
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1, email);
            ps.setString(2, code);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                Timestamp createdAt = rs.getTimestamp("created_at");
                LocalDateTime createdTime = createdAt.toLocalDateTime();
                LocalDateTime now = LocalDateTime.now();

                // Check if within expiry window
                long secondsElapsed = java.time.Duration.between(createdTime, now).getSeconds();
                if (secondsElapsed <= OTP_EXPIRY_SECONDS) {
                    // Mark as used
                    markOTPUsed(email, code);
                    System.out.println("✅ OTP verified for: " + email);
                    return true;
                } else {
                    System.out.println("⏱ OTP expired for: " + email + " (" + secondsElapsed + "s elapsed)");
                    return false;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Mark OTP as used
     */
    private void markOTPUsed(String email, String code) {
        String sql = "UPDATE otp_codes SET used = TRUE WHERE email = ? AND code = ?";
        try {
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1, email);
            ps.setString(2, code);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Clean up old expired OTP records (optional housekeeping)
     */
    public void cleanupExpiredOTPs() {
        String sql = "DELETE FROM otp_codes WHERE used = TRUE OR created_at < DATE_SUB(NOW(), INTERVAL 10 MINUTE)";
        try {
            Statement st = con.createStatement();
            int deleted = st.executeUpdate(sql);
            if (deleted > 0) {
                System.out.println("🧹 Cleaned " + deleted + " old OTP records");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
