package com.esprit.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;
import com.esprit.services.ForumActivityService;

import java.sql.SQLException;

public class ForumRapportController {

    @FXML
    private TextArea reasonArea;

    private int postId;
    private String userKey = "test_user";
    private final ForumActivityService activityService = new ForumActivityService();

    public void setPostId(int postId) {
        this.postId = postId;
    }

    @FXML
    private void handleReport() {
        String reason = reasonArea.getText().trim();

        if (reason.isEmpty()) {
            showAlert("Erreur", "Veuillez entrer une raison");
            return;
        }

        try {
            String result = activityService.reportPost(postId, userKey, reason);

            if ("REPORTED".equals(result)) {
                showAlert("Succès", "Signalement envoyé avec succès");
                ((Stage) reasonArea.getScene().getWindow()).close();
            } else if ("ALREADY_REPORTED".equals(result)) {
                showAlert("Information", "Vous avez déjà signalé ce contenu");
                ((Stage) reasonArea.getScene().getWindow()).close();
            }

        } catch (SQLException e) {
            showAlert("Erreur", "Erreur lors du signalement: " + e.getMessage());
        }
    }

    @FXML
    private void handleCancel() {
        ((Stage) reasonArea.getScene().getWindow()).close();
    }

    private void showAlert(String title, String message) {
        com.esprit.utils.ForumStyledDialog.show(title, message);
    }
}


