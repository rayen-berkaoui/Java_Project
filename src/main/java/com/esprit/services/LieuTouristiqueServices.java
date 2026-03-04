package com.esprit.services;

import com.esprit.entities.LieuTouristique;
import com.esprit.utils.MyDataBase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class LieuTouristiqueServices implements ICrud<LieuTouristique> {

    private Connection getConnection() {
        return MyDataBase.getInstance().getConnection();
    }

    @Override
    public void ajouter(LieuTouristique l) throws SQLException {
        String sql = "INSERT INTO lieu_touristique " +
            "(nom, description, ville, prix, image, statut, id_categorie, id_adresse) " +
            "VALUES (?,?,?,?,?,?,?,?)";

        PreparedStatement ps = getConnection().prepareStatement(sql);
        ps.setString(1, l.getNom());
        ps.setString(2, l.getDescription());
        ps.setString(3, l.getVille());
        ps.setDouble(4, l.getPrix());
        ps.setString(5, l.getImage());
        ps.setInt(6, l.getStatut());
        ps.setInt(7, l.getId_categorie());
        ps.setInt(8, l.getId_adresse());

        ps.executeUpdate();
    }

    @Override
    public void modifier(LieuTouristique l) throws SQLException {
        String sql = "UPDATE lieu_touristique SET nom=?, description=?, ville=?, prix=?, image=?, statut=?, " +
            "id_categorie=?, id_adresse=? WHERE id_lieu=?";

        PreparedStatement ps = getConnection().prepareStatement(sql);
        ps.setString(1, l.getNom());
        ps.setString(2, l.getDescription());
        ps.setString(3, l.getVille());
        ps.setDouble(4, l.getPrix());
        ps.setString(5, l.getImage());
        ps.setInt(6, l.getStatut());
        ps.setInt(7, l.getId_categorie());
        ps.setInt(8, l.getId_adresse());
        ps.setInt(9, l.getId_lieu());

        ps.executeUpdate();
    }

    @Override
    public void supprimer(int id) throws SQLException {
        String sql = "DELETE FROM lieu_touristique WHERE id_lieu=?";
        PreparedStatement ps = getConnection().prepareStatement(sql);
        ps.setInt(1, id);
        ps.executeUpdate();
    }

    @Override
    public List<LieuTouristique> afficher() throws SQLException {
        List<LieuTouristique> lieux = new ArrayList<>();
        String sql = "SELECT * FROM lieu_touristique";
        System.out.println("[DEBUG] LieuTouristiqueServices.afficher() - executing: " + sql);
        Statement st = getConnection().createStatement();
        ResultSet rs = st.executeQuery(sql);

        while (rs.next()) {
            LieuTouristique l = new LieuTouristique();
            l.setId_lieu(rs.getInt("id_lieu"));
            l.setNom(rs.getString("nom"));
            l.setDescription(rs.getString("description"));
            l.setVille(rs.getString("ville"));
            l.setPrix(rs.getDouble("prix"));
            l.setImage(rs.getString("image"));
            l.setStatut(rs.getInt("statut"));
            l.setId_categorie(rs.getInt("id_categorie"));
            l.setId_adresse(rs.getInt("id_adresse"));

            lieux.add(l);
        }
        System.out.println("[DEBUG] LieuTouristiqueServices.afficher() - found " + lieux.size() + " lieux");
        return lieux;
    }

    /**
     * Affichage complet avec catégorie et adresse
     */
    public List<String> afficherAvecCategorieAdresse() throws SQLException {
        List<String> result = new ArrayList<>();
        String sql = "SELECT l.nom AS lieu_nom, l.prix, l.statut, " +
                "c.nom_categorie, a.rue, a.ville, a.longitude, a.latitude, a.altitude " +
                "FROM lieu_touristique l " +
                "JOIN categorie c ON l.id_categorie = c.id_categorie " +
                "JOIN adresse a ON l.id_adresse = a.id_adresse";

        Statement st = getConnection().createStatement();
        ResultSet rs = st.executeQuery(sql);

        while (rs.next()) {
                String s = "Lieu: " + rs.getString("lieu_nom") +
                    ", Prix: " + rs.getDouble("prix") +
                    ", Statut: " + (rs.getInt("statut") == 1 ? "Disponible" : "Indisponible") +
                    ", Catégorie: " + rs.getString("nom_categorie") +
                    ", Adresse: " + rs.getString("rue") + ", " + rs.getString("ville") +
                    ", Coordonnées: (" + rs.getDouble("longitude") + ", " + rs.getDouble("latitude") +
                    "), Altitude: " + rs.getDouble("altitude");
            result.add(s);
        }
        return result;
    }
}
