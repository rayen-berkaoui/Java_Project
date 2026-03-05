package com.esprit.services;

import com.esprit.entities.categorie;
import com.esprit.utils.MyDataBase;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class categorieServices implements ICrud<categorie> {

    private Connection getConnection() {
        return MyDataBase.getInstance().getConnection();
    }

    @Override
    public void ajouter(categorie categorie) throws SQLException {
        String sql = "INSERT INTO categorie (nom_categorie, description, date_creation) VALUES (?,?,?)";
        PreparedStatement ps = getConnection().prepareStatement(sql);
        ps.setString(1, categorie.getNomcategorie());
        ps.setString(2, categorie.getDescription());
        if (categorie.getDateCreation() != null) {
            ps.setDate(3, Date.valueOf(categorie.getDateCreation()));
        } else {
            ps.setDate(3, null);
        }
        ps.executeUpdate();
        System.out.println("Catégorie ajoutée avec succès !");
    }

    @Override
    public void supprimer(int id) throws SQLException {
        String sql = "DELETE FROM `categorie` WHERE `id_categorie`=?";
        PreparedStatement preparedStatement = getConnection().prepareStatement(sql);
        preparedStatement.setInt(1, id);
        preparedStatement.executeUpdate();
        System.out.println("Catégorie supprimée !");
    }

    @Override
    public List<categorie> afficher() throws SQLException {
        List<categorie> categories = new ArrayList<>();
        String sql = "SELECT * FROM categorie";
        System.out.println("[DEBUG] categorieServices.afficher() - executing: " + sql);
        Statement statement = getConnection().createStatement();
        ResultSet rs = statement.executeQuery(sql);

        while (rs.next()) {
            categorie categorie = new categorie();
            categorie.setIdCategorie(rs.getInt("id_categorie"));
            categorie.setNomcategorie(rs.getString("nom_categorie"));
            categorie.setDescription(rs.getString("description"));

            // DATE SQL -> LocalDate
            Date date = rs.getDate("date_creation");
            if (date != null) {
                categorie.setDateCreation(date.toLocalDate());
            }

            categories.add(categorie);
        }
        System.out.println("[DEBUG] categorieServices.afficher() - found " + categories.size() + " categories");
        return categories;
    }

    public void modifier(categorie categorie) throws SQLException {
        String sql = "UPDATE categorie SET nom_categorie = ?, description = ?, date_creation = ? " +
                "WHERE id_categorie = ?";

        PreparedStatement ps = getConnection().prepareStatement(sql);
        ps.setString(1, categorie.getNomcategorie());
        ps.setString(2, categorie.getDescription());
        ps.setDate(3, Date.valueOf(categorie.getDateCreation()));
        ps.setInt(4, categorie.getIdcategorie());

        ps.executeUpdate();
        System.out.println("Catégorie modifiée avec succès !");
    }

}
