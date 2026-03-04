package Controllers;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import org.example.entities.Post;
import org.example.services.ContentModerationService;
import org.example.services.PostService;
import org.example.services.WarningService;
import org.example.utils.MediaUtils;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Set;

public class addpost implements Initializable {

    @FXML
    private TextArea contentArea;
    @FXML
    private TextArea hashtagsArea;
    @FXML
    private Label charCountLabel;
    @FXML
    private Label hashtagCountLabel;
    @FXML
    private Label imageCountLabel;
    @FXML
    private Label videoCountLabel;
    @FXML
    private TextField userKeyField;
    @FXML
    private TextField userEmailField;
    @FXML
    private Label warningStatusLabel;

    @FXML
    private FlowPane imagesFlowPane;

    @FXML
    private FlowPane videosFlowPane;

    private AppNavigator navigator;
    private final PostService postService = new PostService();
    private final ContentModerationService moderationService = new ContentModerationService();
    private final WarningService warningService = new WarningService();
    private Post postToEdit = null;

    public void setNavigator(AppNavigator navigator) {
        this.navigator = navigator;
    }

    private final List<File> selectedImages = new ArrayList<>();
    private final List<File> selectedVideos = new ArrayList<>();

    private static final String[] IMAGE_EXT = {"*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp", "*.webp"};
    private static final String[] VIDEO_EXT = {"*.mp4", "*.webm", "*.avi", "*.mkv", "*.mov"};

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        contentArea.textProperty().addListener((obs, oldVal, newVal) -> {
            int len = newVal == null ? 0 : newVal.length();
            int lines = newVal == null ? 0 : newVal.split("\n", -1).length;
            charCountLabel.setText(len + " caractères · " + lines + " lignes");

            double newHeight = Math.max(200, lines * 22.0 + 40);
            contentArea.setPrefHeight(newHeight);
        });

        hashtagsArea.textProperty().addListener((obs, oldVal, newVal) -> {
            long count = countHashtags(newVal);
            hashtagCountLabel.setText(count > 0 ? count + " hashtag(s)" : "");

            int lines = newVal == null ? 1 : newVal.split("\n", -1).length;
            double newHeight = Math.max(60, lines * 22.0 + 20);
            hashtagsArea.setPrefHeight(newHeight);
        });

        updateMediaCountLabels();
    }

    private long countHashtags(String text) {
        if (text == null || text.isBlank()) return 0;
        return Arrays.stream(text.split("[,\\s#]+"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .count();
    }

    private String deduplicateHashtags(String raw) {
        if (raw == null || raw.isBlank()) return "";
        Set<String> seen = new LinkedHashSet<>();
        List<String> unique = new ArrayList<>();
        for (String s : raw.split("[,\\s#]+")) {
            String t = s.trim().replace("#", "").toLowerCase();
            if (t.isEmpty()) continue;
            if (seen.add(t)) unique.add(t);
        }
        return String.join(", ", unique);
    }

    private void updateMediaCountLabels() {
        imageCountLabel.setText(selectedImages.isEmpty() ? "" : selectedImages.size() + " image(s)");
        videoCountLabel.setText(selectedVideos.isEmpty() ? "" : selectedVideos.size() + " vidéo(s)");
    }

    @FXML
    private void handleAddImages() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Sélectionner des images");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Images", IMAGE_EXT)
        );
        List<File> files = chooser.showOpenMultipleDialog(contentArea.getScene().getWindow());
        if (files != null && !files.isEmpty()) {
            for (File f : files) {
                if (!selectedImages.contains(f)) {
                    selectedImages.add(f);
                }
            }
            refreshImagesPreview();
            updateMediaCountLabels();
        }
    }

    @FXML
    private void handleAddVideos() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Sélectionner des vidéos");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Vidéos", VIDEO_EXT)
        );
        List<File> files = chooser.showOpenMultipleDialog(contentArea.getScene().getWindow());
        if (files != null && !files.isEmpty()) {
            for (File f : files) {
                if (!selectedVideos.contains(f)) {
                    selectedVideos.add(f);
                }
            }
            refreshVideosPreview();
            updateMediaCountLabels();
        }
    }

    private void refreshImagesPreview() {
        imagesFlowPane.getChildren().clear();
        for (int i = 0; i < selectedImages.size(); i++) {
            final int idx = i;
            File f = selectedImages.get(i);
            VBox thumb = createImageThumb(f);
            Button remove = new Button("✕");
            remove.getStyleClass().add("remove-btn");
            remove.setOnAction(e -> {
                selectedImages.remove(idx);
                refreshImagesPreview();
                updateMediaCountLabels();
            });
            VBox box = new VBox(4);
            box.getStyleClass().add("media-thumb");
            box.setAlignment(Pos.TOP_CENTER);
            box.getChildren().addAll(thumb, remove);
            imagesFlowPane.getChildren().add(box);
        }
    }

    private VBox createImageThumb(File f) {
        ImageView iv = new ImageView();
        try {
            Image img = new Image(f.toURI().toString(), 80, 80, true, true);
            iv.setImage(img);
        } catch (Exception ignored) {
        }
        iv.setFitWidth(80);
        iv.setFitHeight(80);
        iv.setPreserveRatio(true);
        VBox v = new VBox(iv);
        v.setPadding(new Insets(4));
        return v;
    }

    private void refreshVideosPreview() {
        videosFlowPane.getChildren().clear();
        for (int i = 0; i < selectedVideos.size(); i++) {
            final int idx = i;
            File f = selectedVideos.get(i);
            Label name = new Label(f.getName().length() > 20 ? f.getName().substring(0, 17) + "..." : f.getName());
            name.getStyleClass().add("post-card-media-label");
            name.setStyle("-fx-text-fill: #FFD700; -fx-font-size: 11;");
            VBox thumb = new VBox(4);
            thumb.getStyleClass().add("media-thumb");
            thumb.setAlignment(Pos.CENTER);
            thumb.setMinSize(100, 60);
            Label icon = new Label("▶");
            icon.setStyle("-fx-font-size: 24; -fx-text-fill: #FFD700;");
            thumb.getChildren().addAll(icon, name);
            Button remove = new Button("✕");
            remove.getStyleClass().add("remove-btn");
            remove.setOnAction(e -> {
                selectedVideos.remove(idx);
                refreshVideosPreview();
                updateMediaCountLabels();
            });
            VBox box = new VBox(4);
            box.getStyleClass().add("media-thumb");
            box.setAlignment(Pos.TOP_CENTER);
            box.getChildren().addAll(thumb, remove);
            videosFlowPane.getChildren().add(box);
        }
    }

    @FXML
    private void handleSave() {

        String userKey = userKeyField != null ? userKeyField.getText() : "";
        if (userKey != null) userKey = userKey.trim();
        String userEmail = userEmailField != null ? userEmailField.getText() : "";
        if (userEmail != null) userEmail = userEmail.trim();

        if (userKey == null || userKey.isEmpty()) {
            showAlert("Pseudo obligatoire", "Veuillez entrer votre pseudo pour publier.");
            return;
        }
        if (userEmail == null || userEmail.isEmpty() || !userEmail.contains("@")) {
            showAlert("Email obligatoire", "Veuillez entrer un email valide pour publier.");
            return;
        }

        try {
            LocalDateTime blockedUntil = warningService.checkBlocked(userKey);
            if (blockedUntil != null) {
                String until = blockedUntil.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
                showAlert("\uD83D\uDEAB Compte bloqué",
                    "Votre compte est bloqué jusqu'au " + until + ".\n" +
                    "Vous ne pouvez pas publier pendant cette période.");
                if (warningStatusLabel != null) {
                    warningStatusLabel.setText("\uD83D\uDEAB Bloqué jusqu'au " + until);
                    warningStatusLabel.setStyle("-fx-text-fill: #FF4444; -fx-font-size: 13; -fx-font-weight: bold;");
                }
                return;
            }
        } catch (SQLException e) {
            System.err.println("[addpost] Block check error: " + e.getMessage());
        }

        String content = contentArea.getText() != null ? contentArea.getText().trim() : "";
        boolean hasContent = !content.isEmpty();
        boolean hasImages = !selectedImages.isEmpty();
        boolean hasVideos = !selectedVideos.isEmpty();

        if (!hasContent && !hasImages && !hasVideos) {
            showAlert("Erreur", "Le post doit contenir du texte, au moins une image ou au moins une vidéo.");
            return;
        }

        if (hasContent) {
            ContentModerationService.ModerationResult result = moderationService.checkPost(content);
            if (!result.isAllowed()) {

                String badWord = result.getMatchedWord();
                WarningService.WarningResult warning = warningService.processViolation(userKey, userEmail, badWord, content);

                String alertTitle;
                String alertIcon;
                switch (warning.getAction()) {
                    case "WARNING_1":
                        alertTitle = "\u26A0\uFE0F Alerte 1 — Premier avertissement";
                        alertIcon = "-fx-text-fill: #FFD700;";
                        break;
                    case "WARNING_2":
                        alertTitle = "\u26A0\uFE0F\u26A0\uFE0F Alerte 2 — Deuxième avertissement";
                        alertIcon = "-fx-text-fill: #FF8C00;";
                        break;
                    case "BLOCKED":
                        alertTitle = "\uD83D\uDEAB Compte bloqué";
                        alertIcon = "-fx-text-fill: #FF4444;";
                        break;
                    default:
                        alertTitle = "Modération";
                        alertIcon = "-fx-text-fill: #FF8C00;";
                }

                showAlert(alertTitle, warning.getAlertMessage() +
                    "\n\nMot détecté : " + (badWord != null ? badWord : "***") +
                    "\nVeuillez modifier votre publication.");

                if (warningStatusLabel != null) {
                    warningStatusLabel.setText(warning.getAlertMessage());
                    warningStatusLabel.setStyle(alertIcon + " -fx-font-size: 12; -fx-font-weight: bold;");
                }
                return;
            }
        }

        try {
            List<String> imagePaths = new ArrayList<>();
            List<String> videoPaths = new ArrayList<>();
            if (hasImages) {
                imagePaths = MediaUtils.copyFilesToUploads(selectedImages);
            }
            if (hasVideos) {
                videoPaths = MediaUtils.copyFilesToUploads(selectedVideos);
            }

            Post post = new Post();
            post.setContent(hasContent ? content : "");
            String hashtagsRaw = hashtagsArea.getText() != null ? hashtagsArea.getText().trim() : "";
            String hashtags = deduplicateHashtags(hashtagsRaw);
            post.setHashtags(hashtags.isEmpty() ? null : hashtags);
            post.setImagePaths(imagePaths);
            post.setVideoPaths(videoPaths);
            post.setCreatedAt(LocalDateTime.now());

            if (postToEdit != null) {
                post.setId(postToEdit.getId());
                post.setUpdatedAt(LocalDateTime.now());
                postService.modifie(postToEdit.getId(), post);
                showAlert("Succès", "Post modifié avec succès");
            } else {
                postService.ajouter(post);
                showAlert("Succès", "Post publié avec succès");
            }

            if (navigator != null) navigator.goBack();

        } catch (IOException e) {
            showAlert("Erreur", "Impossible de copier les fichiers : " + e.getMessage());
        } catch (SQLException e) {
            showAlert("Erreur", "Erreur base de données : " + e.getMessage());
        }
    }

    @FXML
    private void handleCancel() {
        if (navigator != null) navigator.goBack();
    }

    public void setPostToEdit(Post post) {
        this.postToEdit = post;
        selectedImages.clear();
        selectedVideos.clear();
        if (post != null) {
            contentArea.setText(post.getContent() != null ? post.getContent() : "");
            hashtagsArea.setText(post.getHashtags() != null ? post.getHashtags() : "");
            for (String p : post.getImagePaths()) {
                File f = new File(p);
                if (f.exists()) selectedImages.add(f);
            }
            for (String p : post.getVideoPaths()) {
                File f = new File(p);
                if (f.exists()) selectedVideos.add(f);
            }
            refreshImagesPreview();
            refreshVideosPreview();
            updateMediaCountLabels();
        }
    }

    public void setUserInfo(String userKey, String email) {
        if (userKeyField != null && userKey != null) userKeyField.setText(userKey);
        if (userEmailField != null && email != null) userEmailField.setText(email);
    }

    private void showAlert(String title, String message) {
        org.example.utils.StyledDialog.show(title, message);
    }
}
