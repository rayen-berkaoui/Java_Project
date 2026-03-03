package com.esprit.services;

import com.esprit.entities.Reservation;
import com.esprit.utils.MyDataBase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ReservationService {

    private Connection con;

    public ReservationService() {
        con = MyDataBase.getInstance().getConnection();
    }

    // =====================================================
    // GET ALL RESERVATIONS FOR A CLIENT (via panier)
    // =====================================================
    public List<Reservation> getReservationsByClient(int idClient) {
        List<Reservation> list = new ArrayList<>();
        String sql = "SELECT r.*, p.type_service, p.nb_personnes, p.id_etablissement " +
                     "FROM reservation r " +
                     "JOIN panier p ON r.id_panier = p.id_panier " +
                     "WHERE p.id_client = ? " +
                     "ORDER BY r.date_paiement DESC";
        try {
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, idClient);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(mapReservation(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    // =====================================================
    // GET ALL RESERVATIONS
    // =====================================================
    public List<Reservation> getAll() {
        List<Reservation> list = new ArrayList<>();
        String sql = "SELECT r.*, p.type_service, p.nb_personnes, p.id_etablissement, p.id_client " +
                     "FROM reservation r " +
                     "JOIN panier p ON r.id_panier = p.id_panier " +
                     "ORDER BY r.date_paiement DESC";
        try {
            Statement st = con.createStatement();
            ResultSet rs = st.executeQuery(sql);
            while (rs.next()) {
                Reservation rr = mapReservation(rs);
                try { rr.setIdClient(rs.getInt("id_client")); } catch (SQLException ignored) {}
                list.add(rr);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    // =====================================================
    // CREATE RESERVATION FROM PANIER
    // =====================================================
    public boolean ajouter(Reservation r) {
        String sql = "INSERT INTO reservation (id_panier, date_paiement, montant_total, mode_paiement, statut_paiement, code_confirmation) " +
                     "VALUES (?, ?, ?, ?, ?, ?)";
        try {
            PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setInt(1, r.getIdPanier());
            ps.setTimestamp(2, Timestamp.valueOf(r.getDatePaiement()));
            ps.setDouble(3, r.getMontantTotal());
            ps.setString(4, r.getModePaiement());
            ps.setString(5, r.getStatutPaiement() != null ? r.getStatutPaiement() : "En cours de paiement");
            // Generate confirmation code if not already set, and update the object
            String code = r.getCodeConfirmation() != null ? r.getCodeConfirmation() : generateConfirmationCode();
            r.setCodeConfirmation(code);
            ps.setString(6, code);
            int rows = ps.executeUpdate();
            if (rows > 0) {
                ResultSet keys = ps.getGeneratedKeys();
                if (keys.next()) {
                    r.setIdReservation(keys.getInt(1));
                }
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // =====================================================
    // UPDATE RESERVATION
    // =====================================================
    public boolean modifier(Reservation r) {
        String sql = "UPDATE reservation SET mode_paiement = ?, statut_paiement = ?, montant_total = ? WHERE id_reservation = ?";
        try {
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1, r.getModePaiement());
            ps.setString(2, r.getStatutPaiement());
            ps.setDouble(3, r.getMontantTotal());
            ps.setInt(4, r.getIdReservation());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // =====================================================
    // DELETE RESERVATION
    // =====================================================
    public boolean supprimer(int idReservation) {
        String sql = "DELETE FROM reservation WHERE id_reservation = ?";
        try {
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, idReservation);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // =====================================================
    // UPDATE PAYMENT STATUS
    // =====================================================
    public boolean updateStatut(int idReservation, String statut) {
        String sql = "UPDATE reservation SET statut_paiement = ? WHERE id_reservation = ?";
        try {
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1, statut);
            ps.setInt(2, idReservation);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // =====================================================
    // COUNT RESERVATIONS FOR CLIENT
    // =====================================================
    public int countByClient(int idClient) {
        String sql = "SELECT COUNT(*) FROM reservation r JOIN panier p ON r.id_panier = p.id_panier WHERE p.id_client = ?";
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
    // SAVE RATING AND REVIEW
    // =====================================================
    public boolean saveRating(int idReservation, int rating, String comment) {
        String sql = "UPDATE reservation SET rating = ?, review_comment = ? WHERE id_reservation = ?";
        try {
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, rating);
            ps.setString(2, comment);
            ps.setInt(3, idReservation);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // =====================================================
    // GENERATE CONFIRMATION CODE
    // =====================================================
    private String generateConfirmationCode() {
        return "RES-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    // =====================================================
    // MAP RESULT SET TO RESERVATION
    // =====================================================
    private Reservation mapReservation(ResultSet rs) throws SQLException {
        Reservation r = new Reservation();
        r.setIdReservation(rs.getInt("id_reservation"));
        r.setIdPanier(rs.getInt("id_panier"));

        Timestamp ts = rs.getTimestamp("date_paiement");
        if (ts != null) r.setDatePaiement(ts.toLocalDateTime());

        r.setMontantTotal(rs.getDouble("montant_total"));
        r.setModePaiement(rs.getString("mode_paiement"));
        r.setStatutPaiement(rs.getString("statut_paiement"));
        r.setCodeConfirmation(rs.getString("code_confirmation"));

        try {
            r.setTypeService(rs.getString("type_service"));
            r.setNbPersonnes(rs.getInt("nb_personnes"));
        } catch (SQLException ignored) {}
        try { r.setRating(rs.getInt("rating")); } catch (SQLException ignored) {}
        try { r.setReviewComment(rs.getString("review_comment")); } catch (SQLException ignored) {}

        return r;
    }
}
