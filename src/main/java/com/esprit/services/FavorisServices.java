package com.esprit.services;

import com.esprit.utils.MyDataBase;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Service for managing favorite lieux touristiques.
 * Auto-creates the favoris table if it does not exist.
 */
public class FavorisServices {

    private Connection con;

    public FavorisServices() {
        con = MyDataBase.getInstance().getConnection();
        ensureTableExists();
    }

    private void ensureTableExists() {
        if (con == null) return;
        try {
            String sql = "CREATE TABLE IF NOT EXISTS favoris (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "id_lieu INT NOT NULL UNIQUE, " +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                    "FOREIGN KEY (id_lieu) REFERENCES lieu_touristique(id_lieu) ON DELETE CASCADE" +
                    ")";
            con.createStatement().executeUpdate(sql);
        } catch (SQLException e) {
            System.err.println("⚠️ Could not create favoris table: " + e.getMessage());
        }
    }

    /**
     * Add a lieu to favorites.
     */
    public void addFavori(int idLieu) throws SQLException {
        String sql = "INSERT IGNORE INTO favoris (id_lieu) VALUES (?)";
        PreparedStatement ps = con.prepareStatement(sql);
        ps.setInt(1, idLieu);
        ps.executeUpdate();
    }

    /**
     * Remove a lieu from favorites.
     */
    public void removeFavori(int idLieu) throws SQLException {
        String sql = "DELETE FROM favoris WHERE id_lieu = ?";
        PreparedStatement ps = con.prepareStatement(sql);
        ps.setInt(1, idLieu);
        ps.executeUpdate();
    }

    /**
     * Toggle favorite status: add if not present, remove if already favorited.
     * @return true if the lieu is now a favorite, false if it was removed
     */
    public boolean toggleFavori(int idLieu) throws SQLException {
        if (isFavori(idLieu)) {
            removeFavori(idLieu);
            return false;
        } else {
            addFavori(idLieu);
            return true;
        }
    }

    /**
     * Check if a lieu is in favorites.
     */
    public boolean isFavori(int idLieu) throws SQLException {
        String sql = "SELECT COUNT(*) FROM favoris WHERE id_lieu = ?";
        PreparedStatement ps = con.prepareStatement(sql);
        ps.setInt(1, idLieu);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) return rs.getInt(1) > 0;
        return false;
    }

    /**
     * Get all favorited lieu IDs as a Set for fast lookup.
     */
    public Set<Integer> getAllFavoriIds() throws SQLException {
        Set<Integer> ids = new HashSet<>();
        String sql = "SELECT id_lieu FROM favoris";
        Statement st = con.createStatement();
        ResultSet rs = st.executeQuery(sql);
        while (rs.next()) {
            ids.add(rs.getInt("id_lieu"));
        }
        return ids;
    }

    /**
     * Get all favorited lieu IDs as a list (ordered by creation date, newest first).
     */
    public List<Integer> getAllFavoriIdsList() throws SQLException {
        List<Integer> ids = new ArrayList<>();
        String sql = "SELECT id_lieu FROM favoris ORDER BY created_at DESC";
        Statement st = con.createStatement();
        ResultSet rs = st.executeQuery(sql);
        while (rs.next()) {
            ids.add(rs.getInt("id_lieu"));
        }
        return ids;
    }

    /**
     * Get total number of favorites.
     */
    public int countFavoris() throws SQLException {
        String sql = "SELECT COUNT(*) FROM favoris";
        Statement st = con.createStatement();
        ResultSet rs = st.executeQuery(sql);
        if (rs.next()) return rs.getInt(1);
        return 0;
    }
}
