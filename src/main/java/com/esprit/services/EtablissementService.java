package com.esprit.services;

import com.esprit.entities.Etablissement;
import com.esprit.utils.MyDataBase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class EtablissementService implements ICrud<Etablissement> {

    private Connection con;

    public EtablissementService() {
        con = MyDataBase.getInstance().getConnection();
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

    // ========== Backward-compatible methods ==========
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

    // ========== ICrud CREATE ==========
    @Override
    public void ajouter(Etablissement e) throws SQLException {
        int id = ajouterEtRetournerId(e);
        if (id > 0) e.setIdEtablissement(id);
    }

    public int ajouterEtRetournerId(Etablissement e) throws SQLException {
        String sql = "INSERT INTO etablissement " +
                "(nom, description, adresse, ville, telephone, email, horaires, gammePrix, type, latitude, longitude) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, e.getNom());
            ps.setString(2, nullIfBlank(e.getDescription()));
            ps.setString(3, nullIfBlank(e.getAdresse()));
            ps.setString(4, e.getVille());
            ps.setString(5, nullIfBlank(e.getTelephone()));
            ps.setString(6, nullIfBlank(e.getEmail()));
            ps.setString(7, nullIfBlank(e.getHoraires()));
            ps.setString(8, nullIfBlank(e.getGammePrix()));
            ps.setString(9, normalizeType(e.getType()));

            if (e.getLatitude() == null) ps.setNull(10, Types.DOUBLE);
            else ps.setDouble(10, e.getLatitude());

            if (e.getLongitude() == null) ps.setNull(11, Types.DOUBLE);
            else ps.setDouble(11, e.getLongitude());

            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    int id = rs.getInt(1);
                    e.setIdEtablissement(id);
                    return id;
                }
            }
        }
        return -1;
    }

    // ========== ICrud READ ALL ==========
    @Override
    public List<Etablissement> afficher() throws SQLException {
        List<Etablissement> list = new ArrayList<>();
        String sql = "SELECT * FROM etablissement ORDER BY idEtablissement DESC";

        try (PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapEtablissement(rs));
            }
        }
        return list;
    }

    public Etablissement findById(int id) throws SQLException {
        String sql = "SELECT * FROM etablissement WHERE idEtablissement=?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapEtablissement(rs);
            }
        }
        return null;
    }

    // ========== ICrud UPDATE ==========
    @Override
    public void modifier(Etablissement e) throws SQLException {
        String sql = "UPDATE etablissement SET " +
                "nom=?, description=?, adresse=?, ville=?, telephone=?, email=?, horaires=?, gammePrix=?, type=?, latitude=?, longitude=? " +
                "WHERE idEtablissement=?";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, e.getNom());
            ps.setString(2, nullIfBlank(e.getDescription()));
            ps.setString(3, nullIfBlank(e.getAdresse()));
            ps.setString(4, e.getVille());
            ps.setString(5, nullIfBlank(e.getTelephone()));
            ps.setString(6, nullIfBlank(e.getEmail()));
            ps.setString(7, nullIfBlank(e.getHoraires()));
            ps.setString(8, nullIfBlank(e.getGammePrix()));
            ps.setString(9, normalizeType(e.getType()));

            if (e.getLatitude() == null) ps.setNull(10, Types.DOUBLE);
            else ps.setDouble(10, e.getLatitude());

            if (e.getLongitude() == null) ps.setNull(11, Types.DOUBLE);
            else ps.setDouble(11, e.getLongitude());

            ps.setInt(12, e.getIdEtablissement());
            ps.executeUpdate();
        }
    }

    // ========== ICrud DELETE ==========
    @Override
    public void supprimer(int id) throws SQLException {
        String sql = "DELETE FROM etablissement WHERE idEtablissement=?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
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

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, k);
            ps.setString(2, k);
            ps.setString(3, k);
            ps.setString(4, k);
            ps.setString(5, k);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapEtablissement(rs));
            }
        }
        return list;
    }

    // ========== MAPPING ==========
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

        try {
            e.setType(rs.getString("type"));
            double lat = rs.getDouble("latitude");
            e.setLatitude(rs.wasNull() ? null : lat);
            double lon = rs.getDouble("longitude");
            e.setLongitude(rs.wasNull() ? null : lon);
        } catch (SQLException ignored) {
            // columns may not exist yet before migration
        }
        try {
            int idPart = rs.getInt("id_partenaire");
            e.setIdPartenaire(rs.wasNull() ? null : idPart);
        } catch (SQLException ignored) {}
        return e;
    }

    public List<Etablissement> getByPartenaire(int partenaireId) {
        List<Etablissement> list = new ArrayList<>();
        String sql = "SELECT * FROM etablissement WHERE id_partenaire = ? ORDER BY nom";
        try {
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, partenaireId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapEtablissement(rs));
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    public void ajouterWithPartenaire(Etablissement e, int partenaireId) throws SQLException {
        String sql = "INSERT INTO etablissement " +
                "(nom, description, adresse, ville, telephone, email, horaires, gammePrix, type, latitude, longitude, id_partenaire) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, e.getNom());
            ps.setString(2, nullIfBlank(e.getDescription()));
            ps.setString(3, nullIfBlank(e.getAdresse()));
            ps.setString(4, e.getVille());
            ps.setString(5, nullIfBlank(e.getTelephone()));
            ps.setString(6, nullIfBlank(e.getEmail()));
            ps.setString(7, nullIfBlank(e.getHoraires()));
            ps.setString(8, nullIfBlank(e.getGammePrix()));
            ps.setString(9, normalizeType(e.getType()));
            if (e.getLatitude() == null) ps.setNull(10, Types.DOUBLE);
            else ps.setDouble(10, e.getLatitude());
            if (e.getLongitude() == null) ps.setNull(11, Types.DOUBLE);
            else ps.setDouble(11, e.getLongitude());
            ps.setInt(12, partenaireId);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) e.setIdEtablissement(rs.getInt(1));
            }
        }
    }
}
