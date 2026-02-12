package com.esprit.services;

import com.esprit.entities.utilisateur;
import com.esprit.utils.MyDataBase;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class utilisateurServices implements ICrud<utilisateur> {

    Connection con;

    public utilisateurServices() {
        con = MyDataBase.getInstance().getConnection();
    }

    // ========================
    // AJOUTER
    // ========================
    @Override
    public void ajouter(utilisateur u) throws SQLException {
        String sql = "INSERT INTO utilisateur (nom, prenom, email, mot_de_passe, statut, date_creation, nfc_id, role_id) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        PreparedStatement ps = con.prepareStatement(sql);
        ps.setString(1, u.getNom());
        ps.setString(2, u.getPrenom());
        ps.setString(3, u.getEmail());
        ps.setString(4, u.getMotDePasse());
        ps.setString(5, u.getStatut());
        ps.setDate(6, Date.valueOf(u.getDateCreation()));
        ps.setString(7, u.getNfcId());
        ps.setInt(8, u.getRoleId());

        ps.executeUpdate();
        System.out.println("Utilisateur ajouté avec succès !");
    }

    // ========================
    // SUPPRIMER (soft delete)
    // ========================
    @Override
    public void supprimer(int id) throws SQLException {
        String sql = "UPDATE utilisateur SET statut = 'BLOQUE' WHERE id = ?";
        PreparedStatement ps = con.prepareStatement(sql);
        ps.setInt(1, id);
        ps.executeUpdate();
        System.out.println("Utilisateur bloqué !");
    }

    // ========================
    // AFFICHER
    // ========================
    @Override
    public List<utilisateur> afficher() throws SQLException {
        List<utilisateur> utilisateurs = new ArrayList<>();
        String sql = "SELECT * FROM utilisateur";

        Statement st = con.createStatement();
        ResultSet rs = st.executeQuery(sql);

        while (rs.next()) {
            utilisateur u = new utilisateur();
            u.setId(rs.getInt("id"));
            u.setNom(rs.getString("nom"));
            u.setPrenom(rs.getString("prenom"));
            u.setEmail(rs.getString("email"));
            u.setMotDePasse(rs.getString("mot_de_passe"));
            u.setStatut(rs.getString("statut"));
            u.setNfcId(rs.getString("nfc_id"));
            u.setRoleId(rs.getInt("role_id"));

            Date date = rs.getDate("date_creation");
            if (date != null) {
                u.setDateCreation(date.toLocalDate());
            }

            utilisateurs.add(u);
        }
        return utilisateurs;
    }

    // ========================
    // MODIFIER
    // ========================
    public void modifier(utilisateur u) throws SQLException {
        String sql = "UPDATE utilisateur SET nom=?, prenom=?, email=?, mot_de_passe=?, " +
                "statut=?, date_creation=?, nfc_id=?, role_id=? WHERE id=?";

        PreparedStatement ps = con.prepareStatement(sql);
        ps.setString(1, u.getNom());
        ps.setString(2, u.getPrenom());
        ps.setString(3, u.getEmail());
        ps.setString(4, u.getMotDePasse());
        ps.setString(5, u.getStatut());
        ps.setDate(6, Date.valueOf(u.getDateCreation()));
        ps.setString(7, u.getNfcId());
        ps.setInt(8, u.getRoleId());
        ps.setInt(9, u.getId());

        ps.executeUpdate();
        System.out.println("Utilisateur modifié avec succès !");
    }
}