package com.esprit.services;

import com.esprit.entities.Etablissement;
import com.esprit.utils.MyDataBase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class EtablissementServices implements ICrud<Etablissement> {

    private final Connection con;

    public EtablissementServices() {
        con = MyDataBase.getInstance().getConnection();
    }

    @Override
    public void ajouter(Etablissement e) throws SQLException {
        String sql = "INSERT INTO etablissement (nom, adresse, ville, telephone, email, description, horaires, gammePrix, image_url) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, e.getNom());
            ps.setString(2, e.getAdresse());
            ps.setString(3, e.getVille());
            ps.setString(4, e.getTelephone());
            ps.setString(5, e.getEmail());
            ps.setString(6, e.getDescription());
            ps.setString(7, e.getHoraires());
            ps.setString(8, e.getGammePrix());
            ps.setString(9, e.getImageUrl());
            ps.executeUpdate();
        } catch (SQLException ex) {
            // Diagnostic information to help debug 'Unknown column' errors
            try {
                System.err.println("SQL Error while executing: " + sql);
                System.err.println("Params: [nom=" + e.getNom() + ", adresse=" + e.getAdresse() + ", ville=" + e.getVille() + ", telephone=" + e.getTelephone() + ", email=" + e.getEmail() + ", description=" + e.getDescription() + ", horaires=" + e.getHoraires() + ", gammePrix=" + e.getGammePrix() + ", image_url=" + e.getImageUrl() + "]");
            } catch (Exception ignore) {}
            throw ex;
        }
    }

    @Override
    public void supprimer(int id) throws SQLException {
        String sql = "DELETE FROM etablissement WHERE idEtablissement = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    @Override
    public List<Etablissement> afficher() throws SQLException {
        List<Etablissement> etablissements = new ArrayList<>();
        String sql = "SELECT * FROM etablissement";

        try (Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            ResultSetMetaData md = rs.getMetaData();
            int colCount = md.getColumnCount();
            // Build a set of available column labels (lower-cased) for tolerant lookup
            java.util.Set<String> cols = new java.util.HashSet<>();
            for (int i = 1; i <= colCount; i++) {
                cols.add(md.getColumnLabel(i).toLowerCase());
            }

            // Helper to find the actual column name to use from candidates
            java.util.function.Function<String[], String> find = candidates -> {
                for (String c : candidates) {
                    if (c == null) continue;
                    if (cols.contains(c.toLowerCase())) return c;
                }
                return null;
            };

            String colId = find.apply(new String[]{"idEtablissement", "id_etablissement", "id"});
            String colNom = find.apply(new String[]{"nom"});
            String colAdresse = find.apply(new String[]{"adresse"});
            String colVille = find.apply(new String[]{"ville"});
            String colTelephone = find.apply(new String[]{"telephone", "tel"});
            String colEmail = find.apply(new String[]{"email"});
            String colDescription = find.apply(new String[]{"description"});
            String colHoraires = find.apply(new String[]{"horaires"});
            String colGamme = find.apply(new String[]{"gammePrix", "gamme_prix"});
            String colImage = find.apply(new String[]{"image_url", "imageurl", "image"});

            while (rs.next()) {
                Etablissement e = new Etablissement();
                if (colId != null) e.setIdEtablissement(rs.getInt(colId));
                if (colNom != null) e.setNom(rs.getString(colNom));
                if (colAdresse != null) e.setAdresse(rs.getString(colAdresse));
                if (colVille != null) e.setVille(rs.getString(colVille));
                if (colTelephone != null) e.setTelephone(rs.getString(colTelephone));
                if (colEmail != null) e.setEmail(rs.getString(colEmail));
                if (colDescription != null) e.setDescription(rs.getString(colDescription));
                if (colHoraires != null) e.setHoraires(rs.getString(colHoraires));
                if (colGamme != null) e.setGammePrix(rs.getString(colGamme));
                if (colImage != null) e.setImageUrl(rs.getString(colImage));
                etablissements.add(e);
            }
        }
        return etablissements;
    }

    @Override
    public void modifier(Etablissement e) throws SQLException {
        String sql = "UPDATE etablissement SET nom=?, adresse=?, ville=?, telephone=?, email=?, description=?, horaires=?, gammePrix=?, image_url=? " +
                "WHERE idEtablissement=?";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, e.getNom());
            ps.setString(2, e.getAdresse());
            ps.setString(3, e.getVille());
            ps.setString(4, e.getTelephone());
            ps.setString(5, e.getEmail());
            ps.setString(6, e.getDescription());
            ps.setString(7, e.getHoraires());
            ps.setString(8, e.getGammePrix());
            ps.setString(9, e.getImageUrl());
            ps.setInt(10, e.getIdEtablissement());
            ps.executeUpdate();
        }
    }
}
