package com.esprit.services;

import com.esprit.entities.Activite;
import com.esprit.utils.MyDataBase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ActiviteServices {

    private final Connection cnx = MyDataBase.getInstance().getConnection();

    public void ajouter(Activite a) throws SQLException {
        String sql = "INSERT INTO activite (nomActivite, description, categorie, duree, niveau, image_url) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, a.getNomActivite());
            ps.setString(2, a.getDescription());
            ps.setString(3, a.getCategorie());

            if (a.getDuree() == null) ps.setNull(4, Types.INTEGER);
            else ps.setInt(4, a.getDuree());

            ps.setString(5, a.getNiveau());
            ps.setString(6, a.getImageUrl()); // ✅ NEW
            ps.executeUpdate();
        }
    }

    public void modifier(Activite a) throws SQLException {
        String sql = "UPDATE activite SET nomActivite=?, description=?, categorie=?, duree=?, niveau=?, image_url=? WHERE idActivite=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, a.getNomActivite());
            ps.setString(2, a.getDescription());
            ps.setString(3, a.getCategorie());

            if (a.getDuree() == null) ps.setNull(4, Types.INTEGER);
            else ps.setInt(4, a.getDuree());

            ps.setString(5, a.getNiveau());
            ps.setString(6, a.getImageUrl()); // ✅ NEW
            ps.setInt(7, a.getIdActivite());
            ps.executeUpdate();
        }
    }

    public void supprimer(int id) throws SQLException {
        String sql = "DELETE FROM activite WHERE idActivite=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    public List<Activite> afficher() throws SQLException {
        String sql = "SELECT idActivite, nomActivite, description, categorie, duree, niveau, image_url FROM activite";
        List<Activite> list = new ArrayList<>();

        try (Statement st = cnx.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                Activite a = new Activite();
                a.setIdActivite(rs.getInt("idActivite"));
                a.setNomActivite(rs.getString("nomActivite"));
                a.setDescription(rs.getString("description"));
                a.setCategorie(rs.getString("categorie"));

                int d = rs.getInt("duree");
                a.setDuree(rs.wasNull() ? null : d);

                a.setNiveau(rs.getString("niveau"));

                // ✅ NEW
                a.setImageUrl(rs.getString("image_url"));

                list.add(a);
            }
        }
        return list;
    }
}
