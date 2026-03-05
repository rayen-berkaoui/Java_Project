package com.esprit.services;

import com.esprit.entities.Activite;
import com.esprit.utils.MyDataBase;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ActiviteServices implements ICrud<Activite> {

    private final Connection cnx;

    public ActiviteServices() {
        cnx = MyDataBase.getInstance().getConnection();
    }

    private String normalizeEnum(String v, String def) {
        if (v == null || v.isBlank()) return def;
        return v.trim().toLowerCase();
    }

    // ================= CREATE =================
    @Override
    public void ajouter(Activite a) throws SQLException {
        String sql = "INSERT INTO activite (" +
                "nomActivite, description, categorie, duree, niveau, prix, devise, " +
                "date_debut, date_fin, nb_places, places_dispo, adresse_depart, age_min, " +
                "equipement_inclus, conditions_annulation, statut, idEtablissement" +
                ") VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            fillInsertOrUpdate(ps, a, false);
            ps.executeUpdate();
        }
    }

    // ✅ CREATE + retourne ID
    public int ajouterEtRetournerId(Activite a) throws SQLException {
        String sql = "INSERT INTO activite (" +
                "nomActivite, description, categorie, duree, niveau, prix, devise, " +
                "date_debut, date_fin, nb_places, places_dispo, adresse_depart, age_min, " +
                "equipement_inclus, conditions_annulation, statut, idEtablissement" +
                ") VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

        try (PreparedStatement ps = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            fillInsertOrUpdate(ps, a, false);
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        return -1;
    }

    // ================= READ =================
    @Override
    public List<Activite> afficher() throws SQLException {
        List<Activite> list = new ArrayList<>();
        String sql = "SELECT * FROM activite ORDER BY idActivite DESC";

        try (PreparedStatement ps = cnx.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    public Activite findById(int id) throws SQLException {
        String sql = "SELECT * FROM activite WHERE idActivite=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return map(rs);
            }
        }
        return null;
    }

    // ================= UPDATE =================
    @Override
    public void modifier(Activite a) throws SQLException {
        String sql = "UPDATE activite SET " +
                "nomActivite=?, description=?, categorie=?, duree=?, niveau=?, prix=?, devise=?, " +
                "date_debut=?, date_fin=?, nb_places=?, places_dispo=?, adresse_depart=?, age_min=?, " +
                "equipement_inclus=?, conditions_annulation=?, statut=?, idEtablissement=? " +
                "WHERE idActivite=?";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            fillInsertOrUpdate(ps, a, true);
            ps.setInt(18, a.getIdActivite());
            ps.executeUpdate();
        }
    }

    // ================= DELETE =================
    @Override
    public void supprimer(int id) throws SQLException {
        String sql = "DELETE FROM activite WHERE idActivite=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    // ================= Helpers =================
    private void fillInsertOrUpdate(PreparedStatement ps, Activite a, boolean isUpdate) throws SQLException {
        ps.setString(1, a.getNomActivite());

        if (a.getDescription() == null || a.getDescription().isBlank()) ps.setNull(2, Types.VARCHAR);
        else ps.setString(2, a.getDescription());

        ps.setString(3, normalizeEnum(a.getCategorie(), "autre"));

        if (a.getDuree() == null) ps.setNull(4, Types.INTEGER);
        else ps.setInt(4, a.getDuree());

        if (a.getNiveau() == null || a.getNiveau().isBlank()) ps.setNull(5, Types.VARCHAR);
        else ps.setString(5, normalizeEnum(a.getNiveau(), "debutant"));

        if (a.getPrix() == null) ps.setNull(6, Types.DECIMAL);
        else ps.setBigDecimal(6, a.getPrix());

        String devise = (a.getDevise() == null || a.getDevise().isBlank()) ? "TND" : a.getDevise().trim();
        ps.setString(7, devise);

        if (a.getDateDebut() == null) ps.setNull(8, Types.TIMESTAMP);
        else ps.setTimestamp(8, a.getDateDebut());

        if (a.getDateFin() == null) ps.setNull(9, Types.TIMESTAMP);
        else ps.setTimestamp(9, a.getDateFin());

        if (a.getNbPlaces() == null) ps.setNull(10, Types.INTEGER);
        else ps.setInt(10, a.getNbPlaces());

        if (a.getPlacesDispo() == null) ps.setNull(11, Types.INTEGER);
        else ps.setInt(11, a.getPlacesDispo());

        if (a.getAdresseDepart() == null || a.getAdresseDepart().isBlank()) ps.setNull(12, Types.VARCHAR);
        else ps.setString(12, a.getAdresseDepart());

        if (a.getAgeMin() == null) ps.setNull(13, Types.INTEGER);
        else ps.setInt(13, a.getAgeMin());

        if (a.getEquipementInclus() == null || a.getEquipementInclus().isBlank()) ps.setNull(14, Types.VARCHAR);
        else ps.setString(14, a.getEquipementInclus());

        if (a.getConditionsAnnulation() == null || a.getConditionsAnnulation().isBlank()) ps.setNull(15, Types.VARCHAR);
        else ps.setString(15, a.getConditionsAnnulation());

        ps.setString(16, normalizeEnum(a.getStatut(), "disponible"));

        // FK : idEtablissement
        if (a.getIdEtablissement() == null) ps.setNull(17, Types.INTEGER);
        else ps.setInt(17, a.getIdEtablissement());
    }

    private Activite map(ResultSet rs) throws SQLException {
        Activite a = new Activite();

        a.setIdActivite(rs.getInt("idActivite"));
        a.setNomActivite(rs.getString("nomActivite"));
        a.setDescription(rs.getString("description"));
        a.setCategorie(rs.getString("categorie"));

        int d = rs.getInt("duree");
        a.setDuree(rs.wasNull() ? null : d);

        a.setNiveau(rs.getString("niveau"));

        BigDecimal prix = rs.getBigDecimal("prix");
        a.setPrix(prix);

        a.setDevise(rs.getString("devise"));
        a.setDateDebut(rs.getTimestamp("date_debut"));
        a.setDateFin(rs.getTimestamp("date_fin"));

        int nb = rs.getInt("nb_places");
        a.setNbPlaces(rs.wasNull() ? null : nb);

        int dispo = rs.getInt("places_dispo");
        a.setPlacesDispo(rs.wasNull() ? null : dispo);

        a.setAdresseDepart(rs.getString("adresse_depart"));

        int age = rs.getInt("age_min");
        a.setAgeMin(rs.wasNull() ? null : age);

        a.setEquipementInclus(rs.getString("equipement_inclus"));
        a.setConditionsAnnulation(rs.getString("conditions_annulation"));
        a.setStatut(rs.getString("statut"));

        int etabId = rs.getInt("idEtablissement");
        a.setIdEtablissement(rs.wasNull() ? null : etabId);

        return a;
    }
}