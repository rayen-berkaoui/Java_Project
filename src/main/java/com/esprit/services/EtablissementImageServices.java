package com.esprit.services;

import com.esprit.entities.EtablissementImage;
import com.esprit.utils.MyDataBase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class EtablissementImageServices {

    private final Connection cnx;

    public EtablissementImageServices() {
        cnx = MyDataBase.getInstance().getConnection();
    }

    // CREATE: ajouter une image
    public void ajouterImage(EtablissementImage img) throws SQLException {
        String sql = "INSERT INTO etablissement_image (idEtablissement, image_path, ordre_affichage) VALUES (?, ?, ?)";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, img.getIdEtablissement());
            ps.setString(2, img.getImagePath());
            ps.setInt(3, img.getOrdreAffichage());
            ps.executeUpdate();
        }
    }

    // READ: images d'un établissement
    public List<EtablissementImage> getImagesByEtablissement(int idEtablissement) throws SQLException {
        List<EtablissementImage> list = new ArrayList<>();
        String sql = "SELECT * FROM etablissement_image WHERE idEtablissement=? ORDER BY ordre_affichage ASC, idImage ASC";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, idEtablissement);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    EtablissementImage img = new EtablissementImage();
                    img.setIdImage(rs.getInt("idImage"));
                    img.setIdEtablissement(rs.getInt("idEtablissement"));
                    img.setImagePath(rs.getString("image_path"));
                    img.setOrdreAffichage(rs.getInt("ordre_affichage"));
                    list.add(img);
                }
            }
        }
        return list;
    }

    // DELETE: supprimer toutes les images d'un établissement
    public void supprimerImagesByEtablissement(int idEtablissement) throws SQLException {
        String sql = "DELETE FROM etablissement_image WHERE idEtablissement=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, idEtablissement);
            ps.executeUpdate();
        }
    }

    // ✅ (BONUS PRO) DELETE: supprimer une seule image
    public void supprimerImage(int idImage) throws SQLException {
        String sql = "DELETE FROM etablissement_image WHERE idImage=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, idImage);
            ps.executeUpdate();
        }
    }

    // ✅ (BONUS PRO) Définir une image principale (ordre_affichage = 1)
    // Logique: on met d'abord toutes les images à ordre 2, puis l'image choisie à ordre 1
    public void definirImagePrincipale(int idEtablissement, int idImage) throws SQLException {
        String resetSql = "UPDATE etablissement_image SET ordre_affichage = 2 WHERE idEtablissement=?";
        String setSql   = "UPDATE etablissement_image SET ordre_affichage = 1 WHERE idImage=? AND idEtablissement=?";

        try (PreparedStatement ps1 = cnx.prepareStatement(resetSql);
             PreparedStatement ps2 = cnx.prepareStatement(setSql)) {

            cnx.setAutoCommit(false);

            ps1.setInt(1, idEtablissement);
            ps1.executeUpdate();

            ps2.setInt(1, idImage);
            ps2.setInt(2, idEtablissement);
            ps2.executeUpdate();

            cnx.commit();
        } catch (SQLException e) {
            cnx.rollback();
            throw e;
        } finally {
            cnx.setAutoCommit(true);
        }
    }

    // ✅ (OPTIONNEL) Vérifier si une image existe
    public boolean imageExiste(int idImage) throws SQLException {
        String sql = "SELECT 1 FROM etablissement_image WHERE idImage=? LIMIT 1";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, idImage);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }
}