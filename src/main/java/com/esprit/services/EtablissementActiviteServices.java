package com.esprit.services;

import com.esprit.entities.EtablissementActivite;
import com.esprit.utils.MyDataBase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class EtablissementActiviteServices implements ICrud<EtablissementActivite> {

    private final Connection con;

    public EtablissementActiviteServices() {
        con = MyDataBase.getInstance().getConnection();
    }

    @Override
    public void ajouter(EtablissementActivite ea) throws SQLException {
        String sql = "INSERT INTO etablissement_activite (idEtablissement, idActivite, prix, disponible, capacite) VALUES (?, ?, ?, ?, ?)";
        PreparedStatement ps = con.prepareStatement(sql);
        ps.setInt(1, ea.getIdEtablissement());
        ps.setInt(2, ea.getIdActivite());
        ps.setDouble(3, ea.getPrix());
        ps.setBoolean(4, ea.isDisponible());
        ps.setInt(5, ea.getCapacite());
        ps.executeUpdate();
        System.out.println("Association Etablissement-Activite ajoutée !");
    }

    // ⚠️ ICrud n'est pas adapté à une PK composée -> on met une version simple (non utilisée)
    @Override
    public void supprimer(int id) throws SQLException {
        throw new UnsupportedOperationException("Utilise supprimer(int idEtablissement, int idActivite)");
    }

    // ✅ La bonne méthode pour une PK composée
    public void supprimer(int idEtablissement, int idActivite) throws SQLException {
        String sql = "DELETE FROM etablissement_activite WHERE idEtablissement = ? AND idActivite = ?";
        PreparedStatement ps = con.prepareStatement(sql);
        ps.setInt(1, idEtablissement);
        ps.setInt(2, idActivite);
        ps.executeUpdate();
        System.out.println("Association supprimée !");
    }

    @Override
    public List<EtablissementActivite> afficher() throws SQLException {
        List<EtablissementActivite> list = new ArrayList<>();
        String sql = "SELECT * FROM etablissement_activite";
        Statement st = con.createStatement();
        ResultSet rs = st.executeQuery(sql);

        while (rs.next()) {
            EtablissementActivite ea = new EtablissementActivite();
            ea.setIdEtablissement(rs.getInt("idEtablissement"));
            ea.setIdActivite(rs.getInt("idActivite"));
            ea.setPrix(rs.getDouble("prix"));
            ea.setDisponible(rs.getBoolean("disponible"));
            ea.setCapacite(rs.getInt("capacite"));
            list.add(ea);
        }
        return list;
    }

    @Override
    public void modifier(EtablissementActivite ea) throws SQLException {
        String sql = "UPDATE etablissement_activite SET prix = ?, disponible = ?, capacite = ? " +
                "WHERE idEtablissement = ? AND idActivite = ?";
        PreparedStatement ps = con.prepareStatement(sql);
        ps.setDouble(1, ea.getPrix());
        ps.setBoolean(2, ea.isDisponible());
        ps.setInt(3, ea.getCapacite());
        ps.setInt(4, ea.getIdEtablissement());
        ps.setInt(5, ea.getIdActivite());
        ps.executeUpdate();
        System.out.println("Association modifiée !");
    }
}
