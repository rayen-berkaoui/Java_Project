package com.esprit.services;

import com.esprit.entities.ActiviteImage;
import com.esprit.utils.MyDataBase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.sql.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ActiviteImageServices {

    private final Connection cnx;

    public ActiviteImageServices() {
        cnx = MyDataBase.getInstance().getConnection();
    }

    // CREATE: ajouter une image
    public void ajouterImage(ActiviteImage img) throws SQLException {
        String sql = "INSERT INTO activite_image (idActivite, image_path, ordre_affichage) VALUES (?, ?, ?)";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, img.getIdActivite());
            ps.setString(2, img.getImagePath());
            ps.setInt(3, img.getOrdreAffichage());
            ps.executeUpdate();
        }
    }

    // READ: images d'une activité
    public List<ActiviteImage> getImagesByActivite(int idActivite) throws SQLException {
        List<ActiviteImage> list = new ArrayList<>();
        String sql = "SELECT * FROM activite_image WHERE idActivite=? ORDER BY ordre_affichage ASC, idImage ASC";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, idActivite);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ActiviteImage img = new ActiviteImage();
                    img.setIdImage(rs.getInt("idImage"));
                    img.setIdActivite(rs.getInt("idActivite"));
                    img.setImagePath(rs.getString("image_path"));
                    img.setOrdreAffichage(rs.getInt("ordre_affichage"));
                    list.add(img);
                }
            }
        }
        return list;
    }

    // DELETE: supprimer toutes les images d'une activité
    public void supprimerImagesByActivite(int idActivite) throws SQLException {
        String sql = "DELETE FROM activite_image WHERE idActivite=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, idActivite);
            ps.executeUpdate();
        }
    }

    // BONUS: supprimer une image
    public void supprimerImage(int idImage) throws SQLException {
        String sql = "DELETE FROM activite_image WHERE idImage=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, idImage);
            ps.executeUpdate();
        }
    }

    public Map<Integer, String> getCoverMap(List<Integer> activiteIds) throws SQLException {
        Map<Integer, String> map = new HashMap<>();
        if (activiteIds == null || activiteIds.isEmpty()) return map;

        // placeholders ?,?,?
        String in = activiteIds.stream().map(x -> "?").collect(java.util.stream.Collectors.joining(","));

        // ✅ prendre la première image (plus petit ordre_affichage) par activité
        String sql =
                "SELECT ai.idActivite, ai.image_path " +
                        "FROM activite_image ai " +
                        "JOIN (" +
                        "   SELECT idActivite, MIN(ordre_affichage) AS minOrdre " +
                        "   FROM activite_image " +
                        "   WHERE idActivite IN (" + in + ") " +
                        "   GROUP BY idActivite" +
                        ") t ON t.idActivite = ai.idActivite AND t.minOrdre = ai.ordre_affichage";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            int i = 1;
            for (Integer id : activiteIds) ps.setInt(i++, id);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    map.put(rs.getInt("idActivite"), rs.getString("image_path"));
                }
            }
        }
        return map;
    }
}