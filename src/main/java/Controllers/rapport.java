package Controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;
import org.example.services.ActivityService;

import java.sql.SQLException;

public class rapport {

    @FXML
    private TextArea reasonArea;

    private int postId;
    private String userKey = "test_user";
    private final ActivityService activityService = new ActivityService();

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
        org.example.utils.StyledDialog.show(title, message);
    }
}
