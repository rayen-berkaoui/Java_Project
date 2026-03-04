package Controllers;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;
import org.example.entities.Comment;
import org.example.services.CommentService;

import java.net.URL;
import java.sql.SQLException;
import java.util.ResourceBundle;

public class EditComment implements Initializable {
    @FXML
    private TextArea commentArea;
    @FXML
    private Button saveBtn;
    @FXML
    private Button cancelBtn;

    private Comment comment;
    private final CommentService commentService = new CommentService();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        saveBtn.setOnAction(e -> handleSave());
        cancelBtn.setOnAction(e -> ((Stage) commentArea.getScene().getWindow()).close());
    }

    public void setComment(Comment comment) {
        this.comment = comment;
        commentArea.setText(comment.getContent());
    }

    private void handleSave() {
        String content = commentArea.getText().trim();
        if (content.isEmpty()) {
            showAlert("Erreur", "Le commentaire ne peut pas être vide");
            return;
        }
        comment.setContent(content);
        comment.setUpdatedAt(java.time.LocalDateTime.now());
        try {
            commentService.modifier(comment.getId(), comment);
            showAlert("Succès", "Commentaire modifié avec succès");
            ((Stage) commentArea.getScene().getWindow()).close();
        } catch (SQLException e) {
            showAlert("Erreur", "Erreur lors de la modification: " + e.getMessage());
        }
    }

    private void showAlert(String title, String message) {
        org.example.utils.StyledDialog.show(title, message);
    }
}
