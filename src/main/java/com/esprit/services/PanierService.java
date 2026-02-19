package com.esprit.services;

import com.esprit.entities.Panier;
import com.esprit.utils.MyDataBase;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class PanierService {

    private Connection con;

    public PanierService() {
        con = MyDataBase.getInstance().getConnection();
    }

    // =====================================================
    // GET ALL PANIER ITEMS FOR A CLIENT
    // =====================================================
    public List<Panier> getPanierByClient(int idClient) {
        List<Panier> list = new ArrayList<>();
        String sql = "SELECT p.*, u.nom AS nom_client, u.prenom AS prenom_client " +
                     "FROM panier p " +
                     "LEFT JOIN utilisateur u ON p.id_client = u.id " +
                     "WHERE p.id_client = ? " +
                     "ORDER BY p.date_debut DESC";
        try {
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, idClient);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(mapPanier(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    // =====================================================
    // GET ALL PANIER ITEMS
    // =====================================================
    public List<Panier> getAll() {
        List<Panier> list = new ArrayList<>();
        String sql = "SELECT p.*, u.nom AS nom_client, u.prenom AS prenom_client " +
                     "FROM panier p " +
                     "LEFT JOIN utilisateur u ON p.id_client = u.id " +
                     "ORDER BY p.date_debut DESC";
        try {
            Statement st = con.createStatement();
            ResultSet rs = st.executeQuery(sql);
            while (rs.next()) {
                list.add(mapPanier(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    // =====================================================
    // ADD ITEM TO PANIER
    // =====================================================
    public boolean ajouter(Panier p) {
        String sql = "INSERT INTO panier (id_client, id_etablissement, type_service, date_debut, date_fin, nb_personnes, prix_estime, statut_item, nb_adultes, nb_enfants) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try {
            PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setInt(1, p.getIdClient());
            ps.setInt(2, p.getIdEtablissement());
            ps.setString(3, p.getTypeService());
            ps.setTimestamp(4, Timestamp.valueOf(p.getDateDebut()));
            ps.setTimestamp(5, Timestamp.valueOf(p.getDateFin()));
            ps.setInt(6, p.getNbPersonnes());
            ps.setDouble(7, p.getPrixEstime());
            ps.setString(8, p.getStatutItem() != null ? p.getStatutItem() : "en_attente");
            ps.setInt(9, p.getNbAdultes() > 0 ? p.getNbAdultes() : 1);
            ps.setInt(10, p.getNbEnfants());
            int rows = ps.executeUpdate();
            if (rows > 0) {
                ResultSet keys = ps.getGeneratedKeys();
                if (keys.next()) {
                    p.setIdPanier(keys.getInt(1));
                }
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // =====================================================
    // UPDATE PANIER ITEM
    // =====================================================
    public boolean modifier(Panier p) {
        String sql = "UPDATE panier SET id_etablissement = ?, type_service = ?, date_debut = ?, date_fin = ?, " +
                     "nb_personnes = ?, prix_estime = ?, statut_item = ?, nb_adultes = ?, nb_enfants = ? WHERE id_panier = ?";
        try {
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, p.getIdEtablissement());
            ps.setString(2, p.getTypeService());
            ps.setTimestamp(3, Timestamp.valueOf(p.getDateDebut()));
            ps.setTimestamp(4, Timestamp.valueOf(p.getDateFin()));
            ps.setInt(5, p.getNbPersonnes());
            ps.setDouble(6, p.getPrixEstime());
            ps.setString(7, p.getStatutItem());
            ps.setInt(8, p.getNbAdultes() > 0 ? p.getNbAdultes() : 1);
            ps.setInt(9, p.getNbEnfants());
            ps.setInt(10, p.getIdPanier());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // =====================================================
    // DELETE PANIER ITEM (and any linked reservations)
    // =====================================================
    public boolean supprimer(int idPanier) {
        try {
            // First delete any linked reservations (FK constraint)
            String delRes = "DELETE FROM reservation WHERE id_panier = ?";
            PreparedStatement psRes = con.prepareStatement(delRes);
            psRes.setInt(1, idPanier);
            psRes.executeUpdate();

            // Then delete the panier item
            String sql = "DELETE FROM panier WHERE id_panier = ?";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, idPanier);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // =====================================================
    // CANCEL PANIER ITEM
    // =====================================================
    public boolean annuler(int idPanier) {
        String sql = "UPDATE panier SET statut_item = 'annulé' WHERE id_panier = ?";
        try {
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, idPanier);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // =====================================================
    // COUNT ITEMS IN PANIER FOR CLIENT
    // =====================================================
    public int countByClient(int idClient) {
        String sql = "SELECT COUNT(*) FROM panier WHERE id_client = ? AND statut_item = 'en_attente'";
        try {
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, idClient);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    // =====================================================
    // TOTAL PRICE FOR CLIENT PANIER
    // =====================================================
    public double totalByClient(int idClient) {
        String sql = "SELECT SUM(prix_estime) FROM panier WHERE id_client = ? AND statut_item = 'en_attente'";
        try {
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, idClient);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getDouble(1);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    // =====================================================
    // MAP RESULT SET TO PANIER
    // =====================================================
    private Panier mapPanier(ResultSet rs) throws SQLException {
        Panier p = new Panier();
        p.setIdPanier(rs.getInt("id_panier"));
        p.setIdClient(rs.getInt("id_client"));
        p.setIdEtablissement(rs.getInt("id_etablissement"));
        p.setTypeService(rs.getString("type_service"));

        Timestamp tsDebut = rs.getTimestamp("date_debut");
        if (tsDebut != null) p.setDateDebut(tsDebut.toLocalDateTime());

        Timestamp tsFin = rs.getTimestamp("date_fin");
        if (tsFin != null) p.setDateFin(tsFin.toLocalDateTime());

        p.setNbPersonnes(rs.getInt("nb_personnes"));
        p.setPrixEstime(rs.getDouble("prix_estime"));
        p.setStatutItem(rs.getString("statut_item"));

        try { p.setNbAdultes(rs.getInt("nb_adultes")); } catch (SQLException e) { p.setNbAdultes(1); }
        try { p.setNbEnfants(rs.getInt("nb_enfants")); } catch (SQLException e) { p.setNbEnfants(0); }

        try {
            String nom = rs.getString("nom_client");
            String prenom = rs.getString("prenom_client");
            p.setNomClient((nom != null ? nom : "") + " " + (prenom != null ? prenom : ""));
        } catch (SQLException ignored) { /* joined fields may not exist */ }

        return p;
    }
}
