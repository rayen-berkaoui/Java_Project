package com.esprit.services;

import com.esprit.entities.utilisateur;
import com.esprit.utils.MyDataBase;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class utilisateurServices implements ICrud<utilisateur> {

    private Connection con;

    public utilisateurServices() {
        con = MyDataBase.getInstance().getConnection();
    }

    // ========================
    // AJOUTER
    // ========================
    @Override
    public void ajouter(utilisateur u) throws SQLException {

        String sql = "INSERT INTO utilisateur (nom, prenom, email, mot_de_passe, statut, date_creation, nfc_id, role_id) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        PreparedStatement ps = con.prepareStatement(sql);
        ps.setString(1, u.getNom());
        ps.setString(2, u.getPrenom());
        ps.setString(3, u.getEmail());
        ps.setString(4, u.getMotDePasse()); // ⚠️ Later → HASH
        ps.setString(5, u.getStatut());
        ps.setDate(6, Date.valueOf(u.getDateCreation()));
        ps.setString(7, u.getNfcId());
        ps.setInt(8, u.getRoleId());

        ps.executeUpdate();
        System.out.println("✅ Utilisateur ajouté avec succès !");
    }

    // ========================
    // SUPPRIMER (Soft delete)
    // ========================
    @Override
    public void supprimer(int id) throws SQLException {

        String sql = "UPDATE utilisateur SET statut = 'BLOQUE' WHERE id = ?";

        PreparedStatement ps = con.prepareStatement(sql);
        ps.setInt(1, id);

        ps.executeUpdate();
        System.out.println("⛔ Utilisateur bloqué !");
    }

    // ========================
    // AFFICHER
    // ========================
    @Override
    public List<utilisateur> afficher() throws SQLException {

        List<utilisateur> utilisateurs = new ArrayList<>();
        String sql = "SELECT * FROM utilisateur";

        Statement st = con.createStatement();
        ResultSet rs = st.executeQuery(sql);

        while (rs.next()) {

            utilisateur u = new utilisateur();

            u.setId(rs.getInt("id"));
            u.setNom(rs.getString("nom"));
            u.setPrenom(rs.getString("prenom"));
            u.setEmail(rs.getString("email"));
            u.setMotDePasse(rs.getString("mot_de_passe"));
            u.setStatut(rs.getString("statut"));
            u.setNfcId(rs.getString("nfc_id"));
            u.setRoleId(rs.getInt("role_id"));
            u.setNumTel(rs.getInt("num_tel"));
            u.setProfilePicture(rs.getString("profile_picture"));
            u.setFaceEncoding(rs.getString("face_encoding"));

            Date date = rs.getDate("date_creation");
            if (date != null) {
                u.setDateCreation(date.toLocalDate());
            }

            utilisateurs.add(u);
        }

        return utilisateurs;
    }

    // ========================
    // MODIFIER
    // ========================
    @Override
    public void modifier(utilisateur u) throws SQLException {

        String sql = "UPDATE utilisateur SET nom=?, prenom=?, email=?, mot_de_passe=?, " +
                "statut=?, date_creation=?, nfc_id=?, role_id=? WHERE id=?";

        PreparedStatement ps = con.prepareStatement(sql);

        ps.setString(1, u.getNom());
        ps.setString(2, u.getPrenom());
        ps.setString(3, u.getEmail());
        ps.setString(4, u.getMotDePasse());
        ps.setString(5, u.getStatut());
        ps.setDate(6, Date.valueOf(u.getDateCreation()));
        ps.setString(7, u.getNfcId());
        ps.setInt(8, u.getRoleId());
        ps.setInt(9, u.getId());

        ps.executeUpdate();
        System.out.println("✏️ Utilisateur modifié avec succès !");
    }

    // =====================================================
    // ✅ REGISTER USER (for SignupController)
    // =====================================================
    public boolean registerUser(String nom, String prenom, String email,
                                String password, int numTel, String nfcId, int roleId) {

        String sql = "INSERT INTO utilisateur " +
                "(nom, prenom, email, mot_de_passe, statut, num_tel, nfc_id, role_id) " +
                "VALUES (?, ?, ?, ?, 'ACTIF', ?, ?, ?)";

        try {
            PreparedStatement ps = con.prepareStatement(sql);

            ps.setString(1, nom);
            ps.setString(2, prenom);
            ps.setString(3, email);
            ps.setString(4, password);
            ps.setInt(5, numTel);
            ps.setString(6, nfcId); // can be null
            ps.setInt(7, roleId);

            ps.executeUpdate();
            return true;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // =====================================================
    // ✅ LOGIN USER (for LoginController)
    // =====================================================
    public utilisateur loginUser(String email, String password) {

        String sql = "SELECT * FROM utilisateur WHERE email = ? AND mot_de_passe = ? AND statut = 'ACTIF'";

        try {
            PreparedStatement ps = con.prepareStatement(sql);

            ps.setString(1, email);
            ps.setString(2, password);

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {

                utilisateur u = new utilisateur();

                u.setId(rs.getInt("id"));
                u.setNom(rs.getString("nom"));
                u.setPrenom(rs.getString("prenom"));
                u.setEmail(rs.getString("email"));
                u.setStatut(rs.getString("statut"));
                u.setRoleId(rs.getInt("role_id"));
                u.setNfcId(rs.getString("nfc_id"));
                u.setNumTel(rs.getInt("num_tel"));
                u.setProfilePicture(rs.getString("profile_picture"));
                u.setFaceEncoding(rs.getString("face_encoding"));

                Date date = rs.getDate("date_creation");
                if (date != null) {
                    u.setDateCreation(date.toLocalDate());
                }

                return u;
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    // =====================================================
    // ✅ EMAIL EXISTS (helper)
    // =====================================================
    public boolean emailExists(String email) {

        String sql = "SELECT id FROM utilisateur WHERE email = ?";

        try {
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1, email);

            ResultSet rs = ps.executeQuery();
            return rs.next();

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // =====================================================
    // ✅ BLOCK USER
    // =====================================================
    public boolean blockUser(int id) {
        String sql = "UPDATE utilisateur SET statut = 'BLOQUE' WHERE id = ?";
        try {
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // =====================================================
    // ✅ UNBLOCK USER
    // =====================================================
    public boolean unblockUser(int id) {
        String sql = "UPDATE utilisateur SET statut = 'ACTIF' WHERE id = ?";
        try {
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // =====================================================
    // ✅ COUNT USERS BY STATUS
    // =====================================================
    public int countByStatus(String statut) {
        String sql = "SELECT COUNT(*) FROM utilisateur WHERE statut = ?";
        try {
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1, statut);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public int countAll() {
        String sql = "SELECT COUNT(*) FROM utilisateur";
        try {
            Statement st = con.createStatement();
            ResultSet rs = st.executeQuery(sql);
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    // =====================================================
    // ✅ COUNT USERS BY ROLE
    // =====================================================
    public int countByRole(int roleId) {
        String sql = "SELECT COUNT(*) FROM utilisateur WHERE role_id = ?";
        try {
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, roleId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    // =====================================================
    // ✅ GET ROLE NAME BY ID
    // =====================================================
    public String getRoleName(int roleId) {
        String sql = "SELECT nom FROM role WHERE id = ?";
        try {
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, roleId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getString("nom");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return "Unknown";
    }

    // =====================================================
    // ✅ UPDATE USER PROFILE (name, email, phone)
    // =====================================================
    public boolean updateProfile(int id, String prenom, String nom, String email, int numTel) {
        String sql = "UPDATE utilisateur SET prenom=?, nom=?, email=?, num_tel=? WHERE id=?";
        try {
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1, prenom);
            ps.setString(2, nom);
            ps.setString(3, email);
            ps.setInt(4, numTel);
            ps.setInt(5, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // =====================================================
    // ✅ CHANGE PASSWORD
    // =====================================================
    public boolean changePassword(int id, String currentPassword, String newPassword) {
        // Verify current password first
        String checkSql = "SELECT id FROM utilisateur WHERE id = ? AND mot_de_passe = ?";
        try {
            PreparedStatement checkPs = con.prepareStatement(checkSql);
            checkPs.setInt(1, id);
            checkPs.setString(2, currentPassword);
            ResultSet rs = checkPs.executeQuery();
            if (!rs.next()) return false; // wrong current password

            String updateSql = "UPDATE utilisateur SET mot_de_passe = ? WHERE id = ?";
            PreparedStatement updatePs = con.prepareStatement(updateSql);
            updatePs.setString(1, newPassword);
            updatePs.setInt(2, id);
            return updatePs.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // =====================================================
    // ✅ CHECK IF USER IS BLOCKED (for login error msg)
    // =====================================================
    public boolean isUserBlocked(String email, String password) {
        String sql = "SELECT statut FROM utilisateur WHERE email = ? AND mot_de_passe = ?";
        try {
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1, email);
            ps.setString(2, password);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return "BLOQUE".equalsIgnoreCase(rs.getString("statut"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // =====================================================
    // ✅ DELETE ACCOUNT
    // =====================================================
    public boolean deleteAccount(int id) {
        String sql = "DELETE FROM utilisateur WHERE id = ?";
        try {
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // =====================================================
    // ✅ UPDATE PROFILE PICTURE (base64)
    // =====================================================
    public boolean updateProfilePicture(int id, String base64Image) {
        String sql = "UPDATE utilisateur SET profile_picture = ? WHERE id = ?";
        try {
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1, base64Image);
            ps.setInt(2, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // =====================================================
    // ✅ RESET PASSWORD BY EMAIL
    // =====================================================
    public boolean resetPassword(String email, String newPassword) {
        String sql = "UPDATE utilisateur SET mot_de_passe = ? WHERE email = ?";
        try {
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1, newPassword);
            ps.setString(2, email);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // =====================================================
    // ✅ CHECK IF USER IS ADMIN
    // =====================================================
    public boolean isAdmin(int roleId) {
        String sql = "SELECT nom FROM role WHERE id = ?";
        try {
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, roleId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return "ADMIN".equalsIgnoreCase(rs.getString("nom"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // =====================================================
    // ✅ PAGINATED USER LIST
    // =====================================================
    public List<utilisateur> getPage(int page, int pageSize) {
        return getPageSorted(page, pageSize, "id", "DESC");
    }

    public List<utilisateur> getPageSorted(int page, int pageSize, String sortColumn, String sortDirection) {
        List<utilisateur> list = new ArrayList<>();
        // Whitelist sort columns to prevent SQL injection
        String col = switch (sortColumn) {
            case "nom" -> "nom";
            case "email" -> "email";
            case "statut" -> "statut";
            case "date_creation" -> "date_creation";
            case "role_id" -> "role_id";
            default -> "id";
        };
        String dir = "ASC".equalsIgnoreCase(sortDirection) ? "ASC" : "DESC";
        String sql = "SELECT * FROM utilisateur ORDER BY " + col + " " + dir + " LIMIT ? OFFSET ?";
        try {
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, pageSize);
            ps.setInt(2, (page - 1) * pageSize);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapUser(rs));
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    // =====================================================
    // ✅ SEARCH USERS BY ID OR NAME (paginated)
    // =====================================================
    public List<utilisateur> searchUsers(String query, int page, int pageSize) {
        return searchUsersSorted(query, page, pageSize, "id", "DESC");
    }

    public List<utilisateur> searchUsersSorted(String query, int page, int pageSize, String sortColumn, String sortDirection) {
        List<utilisateur> list = new ArrayList<>();
        String col = switch (sortColumn) {
            case "nom" -> "nom";
            case "email" -> "email";
            case "statut" -> "statut";
            case "date_creation" -> "date_creation";
            case "role_id" -> "role_id";
            default -> "id";
        };
        String dir = "ASC".equalsIgnoreCase(sortDirection) ? "ASC" : "DESC";
        String sql;
        boolean isNumeric = query.matches("\\d+");

        if (isNumeric) {
            sql = "SELECT * FROM utilisateur WHERE id = ? OR nom LIKE ? OR prenom LIKE ? ORDER BY " + col + " " + dir + " LIMIT ? OFFSET ?";
        } else {
            sql = "SELECT * FROM utilisateur WHERE nom LIKE ? OR prenom LIKE ? OR email LIKE ? ORDER BY " + col + " " + dir + " LIMIT ? OFFSET ?";
        }

        try {
            PreparedStatement ps = con.prepareStatement(sql);
            if (isNumeric) {
                ps.setInt(1, Integer.parseInt(query));
                ps.setString(2, "%" + query + "%");
                ps.setString(3, "%" + query + "%");
                ps.setInt(4, pageSize);
                ps.setInt(5, (page - 1) * pageSize);
            } else {
                ps.setString(1, "%" + query + "%");
                ps.setString(2, "%" + query + "%");
                ps.setString(3, "%" + query + "%");
                ps.setInt(4, pageSize);
                ps.setInt(5, (page - 1) * pageSize);
            }
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapUser(rs));
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    // =====================================================
    // ✅ COUNT SEARCH RESULTS
    // =====================================================
    public int countSearch(String query) {
        String sql;
        boolean isNumeric = query.matches("\\d+");
        if (isNumeric) {
            sql = "SELECT COUNT(*) FROM utilisateur WHERE id = ? OR nom LIKE ? OR prenom LIKE ?";
        } else {
            sql = "SELECT COUNT(*) FROM utilisateur WHERE nom LIKE ? OR prenom LIKE ? OR email LIKE ?";
        }
        try {
            PreparedStatement ps = con.prepareStatement(sql);
            if (isNumeric) {
                ps.setInt(1, Integer.parseInt(query));
                ps.setString(2, "%" + query + "%");
                ps.setString(3, "%" + query + "%");
            } else {
                ps.setString(1, "%" + query + "%");
                ps.setString(2, "%" + query + "%");
                ps.setString(3, "%" + query + "%");
            }
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) { e.printStackTrace(); }
        return 0;
    }

    // =====================================================
    // ✅ GET SINGLE USER BY ID
    // =====================================================
    public utilisateur getUserById(int id) {
        String sql = "SELECT * FROM utilisateur WHERE id = ?";
        try {
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapUser(rs);
        } catch (SQLException e) { e.printStackTrace(); }
        return null;
    }

    // =====================================================
    // ✅ GET RECENT USERS (last N)
    // =====================================================
    public List<utilisateur> getRecentUsers(int limit) {
        List<utilisateur> list = new ArrayList<>();
        String sql = "SELECT * FROM utilisateur ORDER BY date_creation DESC LIMIT ?";
        try {
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, limit);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapUser(rs));
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    // =====================================================
    // HELPER: Map ResultSet row to utilisateur
    // =====================================================
    private utilisateur mapUser(ResultSet rs) throws SQLException {
        utilisateur u = new utilisateur();
        u.setId(rs.getInt("id"));
        u.setNom(rs.getString("nom"));
        u.setPrenom(rs.getString("prenom"));
        u.setEmail(rs.getString("email"));
        u.setMotDePasse(rs.getString("mot_de_passe"));
        u.setStatut(rs.getString("statut"));
        u.setNfcId(rs.getString("nfc_id"));
        u.setRoleId(rs.getInt("role_id"));
        u.setNumTel(rs.getInt("num_tel"));
        u.setProfilePicture(rs.getString("profile_picture"));
        u.setFaceEncoding(rs.getString("face_encoding"));
        try { u.setFaceConfidence(rs.getDouble("face_confidence")); } catch (SQLException ignored) {}
        try { u.setFaceSamplesCount(rs.getInt("face_samples_count")); } catch (SQLException ignored) {}
        try {
            Date faceLoginDate = rs.getDate("last_face_login");
            if (faceLoginDate != null) u.setLastFaceLogin(faceLoginDate.toLocalDate());
        } catch (SQLException ignored) {}
        Date date = rs.getDate("date_creation");
        if (date != null) u.setDateCreation(date.toLocalDate());
        return u;
    }

    // =====================================================
    // ✅ SAVE FACE ENCODING
    // =====================================================
    public boolean saveFaceEncoding(int userId, String faceEncoding) {
        // Count the number of samples in the encoding
        int samplesCount = faceEncoding != null ? faceEncoding.split("\\|\\|\\|").length : 0;
        String sql = "UPDATE utilisateur SET face_encoding = ?, face_samples_count = ? WHERE id = ?";
        try {
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1, faceEncoding);
            ps.setInt(2, samplesCount);
            ps.setInt(3, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // =====================================================
    // ✅ SAVE FACE ENCODING WITH CONFIDENCE
    // =====================================================
    public boolean saveFaceEncodingWithConfidence(int userId, String faceEncoding, double confidence) {
        int samplesCount = faceEncoding != null ? faceEncoding.split("\\|\\|\\|").length : 0;
        String sql = "UPDATE utilisateur SET face_encoding = ?, face_confidence = ?, face_samples_count = ? WHERE id = ?";
        try {
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1, faceEncoding);
            ps.setDouble(2, confidence);
            ps.setInt(3, samplesCount);
            ps.setInt(4, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // =====================================================
    // ✅ UPDATE LAST FACE LOGIN
    // =====================================================
    public boolean updateLastFaceLogin(int userId, double confidence) {
        String sql = "UPDATE utilisateur SET last_face_login = CURDATE(), face_confidence = ? WHERE id = ?";
        try {
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setDouble(1, confidence);
            ps.setInt(2, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // =====================================================
    // ✅ GET ALL USERS WITH FACE ENCODING
    // =====================================================
    public List<utilisateur> getAllUsersWithFaceEncoding() {
        List<utilisateur> list = new ArrayList<>();
        String sql = "SELECT * FROM utilisateur WHERE face_encoding IS NOT NULL AND statut = 'ACTIF'";
        try {
            Statement st = con.createStatement();
            ResultSet rs = st.executeQuery(sql);
            while (rs.next()) list.add(mapUser(rs));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    // =====================================================
    // ✅ FIND USER BY FACE (compare with all stored faces)
    // =====================================================
    public utilisateur findUserByFace(String capturedEncoding, FaceRecognitionService faceService) {
        List<utilisateur> usersWithFaces = getAllUsersWithFaceEncoding();
        utilisateur bestMatch = null;
        double bestScore = 0.0;

        for (utilisateur u : usersWithFaces) {
            String storedEncoding = u.getFaceEncoding();
            if (storedEncoding == null || storedEncoding.isEmpty()) continue;

            double score = faceService.compareFaces(storedEncoding, capturedEncoding);
            System.out.println("Face match score for " + u.getEmail() + ": " + score);

            if (score > bestScore) {
                bestScore = score;
                bestMatch = u;
            }
        }

        // Use the already computed bestScore instead of calling compareFaces again
        if (bestMatch != null && bestScore >= 0.80) {
            System.out.println("✅ Face matched: " + bestMatch.getEmail() + " (score: " + bestScore + ")");
            return bestMatch;
        }

        System.out.println("❌ No face match found. Best score: " + bestScore);
        return null;
    }
}