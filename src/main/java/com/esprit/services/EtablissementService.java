package com.esprit.services;

import com.esprit.entities.Etablissement;
import com.esprit.utils.MyDataBase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class EtablissementService {

    private Connection con;

    public EtablissementService() {
        con = MyDataBase.getInstance().getConnection();
    }

    public List<Etablissement> getAll() {
        List<Etablissement> list = new ArrayList<>();
        String sql = "SELECT * FROM etablissement ORDER BY nom";
        try {
            Statement st = con.createStatement();
            ResultSet rs = st.executeQuery(sql);
            while (rs.next()) {
                list.add(mapEtablissement(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public Etablissement getById(int id) {
        String sql = "SELECT * FROM etablissement WHERE idEtablissement = ?";
        try {
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapEtablissement(rs);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    private Etablissement mapEtablissement(ResultSet rs) throws SQLException {
        Etablissement e = new Etablissement();
        e.setIdEtablissement(rs.getInt("idEtablissement"));
        e.setNom(rs.getString("nom"));
        e.setDescription(rs.getString("description"));
        e.setAdresse(rs.getString("adresse"));
        e.setVille(rs.getString("ville"));
        e.setTelephone(rs.getString("telephone"));
        e.setEmail(rs.getString("email"));
        e.setHoraires(rs.getString("horaires"));
        e.setGammePrix(rs.getString("gammePrix"));
        return e;
    }
}
