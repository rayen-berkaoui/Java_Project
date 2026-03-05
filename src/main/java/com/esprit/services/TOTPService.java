package com.esprit.services;

import com.esprit.utils.MyDataBase;
import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;
import com.warrenstrange.googleauth.GoogleAuthenticatorQRGenerator;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import javafx.scene.image.Image;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.*;

/**
 * TOTP (Time-based One-Time Password) Service for Google Authenticator 2FA.
 *
 * Flow:
 * 1. User enables 2FA → generateSecretKey() → store in DB
 * 2. Show QR code → generateQRCodeImage() → user scans with Google Authenticator
 * 3. On login → verifyCode() with the 6-digit code from the app
 */
public class TOTPService {

    private final GoogleAuthenticator gAuth;
    private final Connection con;

    private static final String ISSUER = "Tabaani";

    public TOTPService() {
        gAuth = new GoogleAuthenticator();
        con = MyDataBase.getInstance().getConnection();
    }

    // =====================================================
    // ✅ GENERATE A NEW SECRET KEY FOR A USER
    // =====================================================
    public String generateSecretKey() {
        GoogleAuthenticatorKey key = gAuth.createCredentials();
        return key.getKey();
    }

    // =====================================================
    // ✅ VERIFY A TOTP CODE AGAINST A SECRET KEY
    // =====================================================
    public boolean verifyCode(String secretKey, int code) {
        if (secretKey == null || secretKey.isEmpty()) return false;
        return gAuth.authorize(secretKey, code);
    }

    // =====================================================
    // ✅ GENERATE THE otpauth:// URI FOR QR CODE
    // =====================================================
    public String getTOTPUri(String secretKey, String userEmail) {
        return GoogleAuthenticatorQRGenerator.getOtpAuthTotpURL(ISSUER, userEmail, 
            new GoogleAuthenticatorKey.Builder(secretKey).build());
    }

    // =====================================================
    // ✅ GENERATE QR CODE AS JavaFX Image
    // =====================================================
    public Image generateQRCodeImage(String secretKey, String userEmail, int width, int height) 
            throws WriterException, IOException {
        String uri = getTOTPUri(secretKey, userEmail);
        QRCodeWriter qrWriter = new QRCodeWriter();
        BitMatrix matrix = qrWriter.encode(uri, BarcodeFormat.QR_CODE, width, height);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(matrix, "PNG", baos);
        return new Image(new ByteArrayInputStream(baos.toByteArray()));
    }

    // =====================================================
    // ✅ SAVE 2FA SECRET TO DATABASE
    // =====================================================
    public boolean saveSecret(int userId, String secretKey) {
        String sql = "UPDATE utilisateur SET totp_secret = ?, totp_enabled = TRUE WHERE id = ?";
        try {
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1, secretKey);
            ps.setInt(2, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // =====================================================
    // ✅ DISABLE 2FA FOR A USER
    // =====================================================
    public boolean disable2FA(int userId) {
        String sql = "UPDATE utilisateur SET totp_secret = NULL, totp_enabled = FALSE WHERE id = ?";
        try {
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // =====================================================
    // ✅ CHECK IF 2FA IS ENABLED FOR A USER
    // =====================================================
    public boolean is2FAEnabled(int userId) {
        String sql = "SELECT totp_enabled FROM utilisateur WHERE id = ?";
        try {
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getBoolean("totp_enabled");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // =====================================================
    // ✅ GET SECRET KEY FOR A USER
    // =====================================================
    public String getSecret(int userId) {
        String sql = "SELECT totp_secret FROM utilisateur WHERE id = ?";
        try {
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getString("totp_secret");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
}
