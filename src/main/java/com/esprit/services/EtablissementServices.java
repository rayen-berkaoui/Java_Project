package com.esprit.services;

import com.esprit.entities.Etablissement;
import com.esprit.utils.MyDataBase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class EtablissementServices implements ICrud<Etablissement> {

    private final Connection cnx;

    public EtablissementServices() {
        cnx = MyDataBase.getInstance().getConnection();
    }

    private String normalizeType(String type) {
        if (type == null || type.isBlank()) return "autre";
        return type.trim().toLowerCase();
    }

    private String nullIfBlank(String s) {
        if (s == null) return null;
        String v = s.trim();
        return v.isEmpty() ? null : v;
    }

    // ========== CREATE ==========
    @Override
    public void ajouter(Etablissement e) throws SQLException {
        // si tu veux ajouter mais sans récupérer id
        int id = ajouterEtRetournerId(e);
        if (id > 0) e.setIdEtablissement(id);
    }

    // ✅ CREATE + retourne l'id (utile pour ajouter images après)
    public int ajouterEtRetournerId(Etablissement e) throws SQLException {
        String sql = "INSERT INTO etablissement " +
                "(nom, description, adresse, ville, telephone, email, horaires, gammePrix, type) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement ps = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, e.getNom());
            ps.setString(2, nullIfBlank(e.getDescription()));
            ps.setString(3, nullIfBlank(e.getAdresse()));
            ps.setString(4, e.getVille());
            ps.setString(5, nullIfBlank(e.getTelephone()));
            ps.setString(6, nullIfBlank(e.getEmail()));
            ps.setString(7, nullIfBlank(e.getHoraires()));
            ps.setString(8, nullIfBlank(e.getGammePrix()));
            ps.setString(9, normalizeType(e.getType()));

            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    int id = rs.getInt(1);
                    e.setIdEtablissement(id); // ✅ super important
                    return id;
                }
            }
        }
        return -1;
    }

    // ========== READ ALL ==========
    @Override
    public List<Etablissement> afficher() throws SQLException {
        List<Etablissement> list = new ArrayList<>();
        String sql = "SELECT * FROM etablissement ORDER BY idEtablissement DESC";

        try (PreparedStatement ps = cnx.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(map(rs));
            }
        }
        return list;
    }

    // ========== READ BY ID ==========
    public Etablissement findById(int id) throws SQLException {
        String sql = "SELECT * FROM etablissement WHERE idEtablissement=?";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return map(rs);
            }
        }
        return null;
    }

    // ========== UPDATE ==========
    @Override
    public void modifier(Etablissement e) throws SQLException {
        String sql = "UPDATE etablissement SET " +
                "nom=?, description=?, adresse=?, ville=?, telephone=?, email=?, horaires=?, gammePrix=?, type=? " +
                "WHERE idEtablissement=?";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, e.getNom());
            ps.setString(2, nullIfBlank(e.getDescription()));
            ps.setString(3, nullIfBlank(e.getAdresse()));
            ps.setString(4, e.getVille());
            ps.setString(5, nullIfBlank(e.getTelephone()));
            ps.setString(6, nullIfBlank(e.getEmail()));
            ps.setString(7, nullIfBlank(e.getHoraires()));
            ps.setString(8, nullIfBlank(e.getGammePrix()));
            ps.setString(9, normalizeType(e.getType()));

            ps.setInt(10, e.getIdEtablissement());

            ps.executeUpdate();
        }
    }

    // ========== DELETE ==========
    @Override
    public void supprimer(int id) throws SQLException {
        String sql = "DELETE FROM etablissement WHERE idEtablissement=?";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    // ========== SEARCH ==========
    public List<Etablissement> search(String keyword) throws SQLException {
        List<Etablissement> list = new ArrayList<>();
        String k = "%" + (keyword == null ? "" : keyword.trim()) + "%";

        String sql = "SELECT * FROM etablissement " +
                "WHERE nom LIKE ? OR ville LIKE ? OR email LIKE ? OR telephone LIKE ? OR type LIKE ? " +
                "ORDER BY idEtablissement DESC";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, k);
            ps.setString(2, k);
            ps.setString(3, k);
            ps.setString(4, k);
            ps.setString(5, k);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        }
        return list;
    }

    // ========== MAPPING ==========
    private Etablissement map(ResultSet rs) throws SQLException {
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
        e.setType(rs.getString("type"));

        return e;
    }
}