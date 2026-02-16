package com.esprit.services;

import com.esprit.entities.LieuImage;
import com.esprit.utils.MyDataBase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for managing multiple images per LieuTouristique.
 * Auto-creates the lieu_image table if it does not exist.
 */
public class LieuImageServices {

    private Connection con;

    public LieuImageServices() {
        con = MyDataBase.getInstance().getConnection();
        ensureTableExists();
    }

    /**
     * Creates the lieu_image table if it doesn't already exist.
     */
    private void ensureTableExists() {
        if (con == null) return;
        try {
            String sql = "CREATE TABLE IF NOT EXISTS lieu_image (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "id_lieu INT NOT NULL, " +
                    "image_path VARCHAR(500) NOT NULL, " +
                    "FOREIGN KEY (id_lieu) REFERENCES lieu_touristique(id_lieu) ON DELETE CASCADE" +
                    ")";
            con.createStatement().executeUpdate(sql);
        } catch (SQLException e) {
            System.err.println("⚠️ Could not create lieu_image table: " + e.getMessage());
        }
    }

    /**
     * Add a single image for a lieu.
     */
    public void ajouter(LieuImage img) throws SQLException {
        String sql = "INSERT INTO lieu_image (id_lieu, image_path) VALUES (?, ?)";
        PreparedStatement ps = con.prepareStatement(sql);
        ps.setInt(1, img.getIdLieu());
        ps.setString(2, img.getImagePath());
        ps.executeUpdate();
    }

    /**
     * Add multiple images for a lieu.
     */
    public void ajouterMultiple(int idLieu, List<String> imagePaths) throws SQLException {
        String sql = "INSERT INTO lieu_image (id_lieu, image_path) VALUES (?, ?)";
        PreparedStatement ps = con.prepareStatement(sql);
        for (String path : imagePaths) {
            ps.setInt(1, idLieu);
            ps.setString(2, path);
            ps.addBatch();
        }
        ps.executeBatch();
    }

    /**
     * Get all images for a given lieu.
     */
    public List<LieuImage> getByLieuId(int idLieu) throws SQLException {
        List<LieuImage> images = new ArrayList<>();
        String sql = "SELECT * FROM lieu_image WHERE id_lieu = ? ORDER BY id";
        PreparedStatement ps = con.prepareStatement(sql);
        ps.setInt(1, idLieu);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            images.add(new LieuImage(
                    rs.getInt("id"),
                    rs.getInt("id_lieu"),
                    rs.getString("image_path")
            ));
        }
        return images;
    }

    /**
     * Delete a single image by ID.
     */
    public void supprimer(int id) throws SQLException {
        String sql = "DELETE FROM lieu_image WHERE id = ?";
        PreparedStatement ps = con.prepareStatement(sql);
        ps.setInt(1, id);
        ps.executeUpdate();
    }

    /**
     * Delete all images for a given lieu.
     */
    public void supprimerByLieuId(int idLieu) throws SQLException {
        String sql = "DELETE FROM lieu_image WHERE id_lieu = ?";
        PreparedStatement ps = con.prepareStatement(sql);
        ps.setInt(1, idLieu);
        ps.executeUpdate();
    }

    /**
     * Replace all images for a lieu (delete old, insert new).
     */
    public void replaceAll(int idLieu, List<String> imagePaths) throws SQLException {
        supprimerByLieuId(idLieu);
        if (imagePaths != null && !imagePaths.isEmpty()) {
            ajouterMultiple(idLieu, imagePaths);
        }
    }

    /**
     * Get the last inserted ID (for new lieux).
     */
    public int getLastInsertedLieuId() throws SQLException {
        String sql = "SELECT MAX(id_lieu) as last_id FROM lieu_touristique";
        Statement st = con.createStatement();
        ResultSet rs = st.executeQuery(sql);
        if (rs.next()) return rs.getInt("last_id");
        return -1;
    }
}
