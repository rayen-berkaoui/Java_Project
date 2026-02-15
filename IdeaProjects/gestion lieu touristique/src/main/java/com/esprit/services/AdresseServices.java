package com.esprit.services;

import com.esprit.entities.Adresse;
import com.esprit.utils.MyDataBase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AdresseServices implements ICrud<Adresse> {

    Connection con;

    public AdresseServices() {
        con = MyDataBase.getInstance().getConnection();
    }

    @Override
    public void ajouter(Adresse a) throws SQLException {
        String sql = "INSERT INTO adresse (rue, ville, latitude, longitude, altitude) VALUES (?,?,?,?,?)";
        PreparedStatement ps = con.prepareStatement(sql);
        ps.setString(1, a.getRue());
        ps.setString(2, a.getVille());
        ps.setDouble(3, a.getLatitude());
        ps.setDouble(4, a.getLongitude());
        ps.setDouble(5, a.getAltitude());
        ps.executeUpdate();
        System.out.println("✅ Adresse ajoutée");
    }

    @Override
    public void modifier(Adresse a) throws SQLException {
        String sql = "UPDATE adresse SET rue=?, ville=?, latitude=?, longitude=?, altitude=? WHERE id_adresse=?";
        PreparedStatement ps = con.prepareStatement(sql);
        ps.setString(1, a.getRue());
        ps.setString(2, a.getVille());
        ps.setDouble(3, a.getLatitude());
        ps.setDouble(4, a.getLongitude());
        ps.setDouble(5, a.getAltitude());
        ps.setInt(6, a.getId_adresse());
        ps.executeUpdate();
        System.out.println("✏️ Adresse modifiée");
    }

    @Override
    public void supprimer(int id) throws SQLException {
        String sql = "DELETE FROM adresse WHERE id_adresse=?";
        PreparedStatement ps = con.prepareStatement(sql);
        ps.setInt(1, id);
        ps.executeUpdate();
        System.out.println("🗑️ Adresse supprimée");
    }

    @Override
    public List<Adresse> afficher() throws SQLException {
        List<Adresse> adresses = new ArrayList<>();
        String sql = "SELECT * FROM adresse";
        Statement st = con.createStatement();
        ResultSet rs = st.executeQuery(sql);

        while (rs.next()) {
            Adresse a = new Adresse(
                    rs.getInt("id_adresse"),
                    rs.getString("rue"),
                    rs.getString("ville"),
                    rs.getDouble("latitude"),
                    rs.getDouble("longitude"),
                    rs.getDouble("altitude")
            );
            adresses.add(a);
        }
        return adresses;
    }
}
