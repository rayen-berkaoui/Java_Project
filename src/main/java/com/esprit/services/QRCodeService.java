package com.esprit.services;

import com.esprit.entities.LieuTouristique;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import javax.imageio.ImageIO;

/**
 * Service for generating QR codes for tourist places.
 * The QR code contains a Google Maps link + place details.
 */
public class QRCodeService {

    private static final int QR_SIZE = 300;

    /**
     * Generate a QR code image for a tourist place.
     * Contains Google Maps coordinates link + place info.
     */
    public static Image generateQRCode(LieuTouristique lieu, double latitude, double longitude) {
        String content = buildQRContent(lieu, latitude, longitude);
        try {
            BufferedImage bufferedImage = createQRImage(content, QR_SIZE);
            return SwingFXUtils.toFXImage(bufferedImage, null);
        } catch (WriterException e) {
            System.err.println("❌ QR Code generation failed: " + e.getMessage());
            return null;
        }
    }

    /**
     * Generate and save QR code to a file.
     */
    public static File saveQRCode(LieuTouristique lieu, double latitude, double longitude, File outputFile) {
        String content = buildQRContent(lieu, latitude, longitude);
        try {
            BufferedImage bufferedImage = createQRImage(content, QR_SIZE);
            ImageIO.write(bufferedImage, "PNG", outputFile);
            System.out.println("✅ QR Code saved to: " + outputFile.getAbsolutePath());
            return outputFile;
        } catch (Exception e) {
            System.err.println("❌ QR Code save failed: " + e.getMessage());
            return null;
        }
    }

    /**
     * Build the QR code content string with Google Maps link.
     */
    private static String buildQRContent(LieuTouristique lieu, double latitude, double longitude) {
        StringBuilder sb = new StringBuilder();
        // Google Maps link for easy navigation
        sb.append("https://www.google.com/maps?q=")
          .append(latitude).append(",").append(longitude);
        sb.append("\n\n");
        sb.append("📍 ").append(lieu.getNom() != null ? lieu.getNom() : "Lieu Touristique");
        if (lieu.getVille() != null) {
            sb.append("\n🏙️ ").append(lieu.getVille());
        }
        sb.append("\n💰 ").append(String.format("%.0f TND", lieu.getPrix()));
        if (lieu.getDescription() != null && !lieu.getDescription().isEmpty()) {
            String desc = lieu.getDescription();
            if (desc.length() > 100) desc = desc.substring(0, 97) + "...";
            sb.append("\n📝 ").append(desc);
        }
        return sb.toString();
    }

    /**
     * Create QR code BufferedImage with custom colors (dark theme matching).
     */
    private static BufferedImage createQRImage(String content, int size) throws WriterException {
        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        hints.put(EncodeHintType.MARGIN, 2);

        QRCodeWriter writer = new QRCodeWriter();
        BitMatrix bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, size, size, hints);

        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Dark background matching app theme
        g.setColor(new java.awt.Color(10, 10, 18));
        g.fillRect(0, 0, size, size);

        // Gold QR code modules
        g.setColor(new java.awt.Color(191, 162, 0));
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                if (bitMatrix.get(x, y)) {
                    g.fillRect(x, y, 1, 1);
                }
            }
        }
        g.dispose();
        return image;
    }
}
