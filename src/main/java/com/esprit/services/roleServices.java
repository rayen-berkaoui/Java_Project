package com.esprit.services;

import com.esprit.entities.role;
import com.esprit.utils.MyDataBase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class roleServices implements ICrud<role> {

    Connection con;

    public roleServices() {
        con = MyDataBase.getInstance().getConnection();
    }

    // ========================
    // AJOUTER
    // ========================
    @Override
    public void ajouter(role r) throws SQLException {
        String sql = "INSERT INTO role (nom, description) VALUES (?, ?)";
        PreparedStatement ps = con.prepareStatement(sql);
        ps.setString(1, r.getNom());
        ps.setString(2, r.getDescription());
        ps.executeUpdate();

        System.out.println("Rôle ajouté avec succès !");
    }

    // ========================
    // SUPPRIMER
    // ========================
    @Override
    public void supprimer(int id) throws SQLException {
        String sql = "DELETE FROM role WHERE id = ?";
        PreparedStatement ps = con.prepareStatement(sql);
        ps.setInt(1, id);
        ps.executeUpdate();

        System.out.println("Rôle supprimé !");
    }

    // ========================
    // AFFICHER
    // ========================
    @Override
    public List<role> afficher() throws SQLException {
        List<role> roles = new ArrayList<>();
        String sql = "SELECT * FROM role";

        Statement st = con.createStatement();
        ResultSet rs = st.executeQuery(sql);

        while (rs.next()) {
            role r = new role();
            r.setId(rs.getInt("id"));
            r.setNom(rs.getString("nom"));
            r.setDescription(rs.getString("description"));
            roles.add(r);
        }
        return roles;
    }

    // ========================
    // MODIFIER
    // ========================
    public void modifier(role r) throws SQLException {
        String sql = "UPDATE role SET nom=?, description=? WHERE id=?";
        PreparedStatement ps = con.prepareStatement(sql);
        ps.setString(1, r.getNom());
        ps.setString(2, r.getDescription());
        ps.setInt(3, r.getId());

        ps.executeUpdate();
        System.out.println("Rôle modifié avec succès !");
    }
}