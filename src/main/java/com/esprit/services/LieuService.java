package com.esprit.services;

import com.esprit.entities.Lieu;
import com.esprit.utils.MyDataBase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class LieuService {

    private Connection con;

    public LieuService() {
        con = MyDataBase.getInstance().getConnection();
    }

    public List<Lieu> getAll() {
        List<Lieu> list = new ArrayList<>();
        String sql = "SELECT l.*, c.nom_categorie FROM lieu l " +
                     "LEFT JOIN categorie c ON l.id_categorie = c.id_categorie " +
                     "WHERE l.statut = 1 ORDER BY l.nom";
        try {
            Statement st = con.createStatement();
            ResultSet rs = st.executeQuery(sql);
            while (rs.next()) {
                list.add(mapLieu(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public Lieu getById(int id) {
        String sql = "SELECT l.*, c.nom_categorie FROM lieu l " +
                     "LEFT JOIN categorie c ON l.id_categorie = c.id_categorie " +
                     "WHERE l.id_lieu = ?";
        try {
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapLieu(rs);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    private Lieu mapLieu(ResultSet rs) throws SQLException {
        Lieu l = new Lieu();
        l.setIdLieu(rs.getInt("id_lieu"));
        l.setNom(rs.getString("nom"));
        l.setDescription(rs.getString("description"));
        l.setVille(rs.getString("ville"));
        l.setPrix(rs.getDouble("prix"));
        l.setImage(rs.getString("image"));
        l.setStatut(rs.getBoolean("statut"));
        l.setIdCategorie(rs.getInt("id_categorie"));
        l.setIdAdresse(rs.getInt("id_adresse"));
        try { l.setNomCategorie(rs.getString("nom_categorie")); } catch (SQLException ignored) {}
        return l;
    }
}
