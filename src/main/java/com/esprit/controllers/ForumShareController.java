package com.esprit.controllers;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import com.esprit.entities.ForumPost;
import com.esprit.services.ForumActivityService;
import com.esprit.services.ForumPostService;

import java.awt.*;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.ResourceBundle;

public class ForumShareController implements Initializable {

    @FXML
    private FlowPane platformsFlowPane;
    private int postId;
    private String userKey = "test_user";
    private Stage stage;
    private final ForumPostService postService = new ForumPostService();
    private final ForumActivityService activityService = new ForumActivityService();

    public void setPostId(int postId) {
        this.postId = postId;
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        platformsFlowPane.getChildren().clear();
        platformsFlowPane.getChildren().addAll(
                createShareButton("f", "Facebook", "#1877F2", this::handleShareFacebook),
                createShareButton("X", "X", "#000000", true, this::handleShareTwitter),
                createShareButtonWithImage("/images/social/whatsapp.png", "Wa", "WhatsApp", "#25D366", this::handleShareWhatsApp),
                createShareButtonWithImage("/images/social/instagram.png", "IG", "Instagram", "#E4405F", this::handleShareInstagram),
                createShareButton("in", "LinkedIn", "#0A66C2", this::handleShareLinkedIn)
        );
    }

    private Button createShareButtonWithImage(String imagePath, String fallbackIcon, String name, String borderColor, Runnable action) {
        HBox content = new HBox(10);
        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(8, 16, 8, 16));

        try {
            java.io.InputStream stream = getClass().getResourceAsStream(imagePath);
            if (stream != null) {
                Image img = new Image(stream, 28, 28, true, true);
                ImageView iv = new ImageView(img);
                iv.setPreserveRatio(true);
                content.getChildren().add(iv);
            } else {
                Label fallback = new Label(fallbackIcon);
                fallback.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
                fallback.setStyle("-fx-text-fill: " + borderColor + ";");
                content.getChildren().add(fallback);
            }
        } catch (Exception e) {
            Label fallback = new Label(fallbackIcon);
            fallback.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
            fallback.setStyle("-fx-text-fill: " + borderColor + ";");
            content.getChildren().add(fallback);
        }

        Label textLabel = new Label(name);
        textLabel.setStyle("-fx-text-fill: #e8e8e8; -fx-font-weight: bold; -fx-font-size: 13;");
        content.getChildren().add(textLabel);

        Button btn = new Button();
        btn.setGraphic(content);
        btn.getStyleClass().add("share-platform-btn");
        btn.setStyle("-fx-background-color: rgba(30,30,30,0.95); -fx-background-radius: 10; " +
                "-fx-border-color: " + borderColor + "; -fx-border-width: 2; -fx-border-radius: 10; -fx-cursor: hand;");
        btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color: rgba(45,45,45,0.98); -fx-background-radius: 10; " +
                "-fx-border-color: " + borderColor + "; -fx-border-width: 2; -fx-border-radius: 10; -fx-cursor: hand;"));
        btn.setOnMouseExited(e -> btn.setStyle("-fx-background-color: rgba(30,30,30,0.95); -fx-background-radius: 10; " +
                "-fx-border-color: " + borderColor + "; -fx-border-width: 2; -fx-border-radius: 10; -fx-cursor: hand;"));
        btn.setOnAction(ev -> action.run());
        return btn;
    }

    private Button createShareButton(String icon, String name, String color, Runnable action) {
        return createShareButton(icon, name, color, false, action);
    }

    private Button createShareButton(String icon, String name, String color, boolean lightBg, Runnable action) {
        Label iconLabel = new Label(icon);
        iconLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 20));
        iconLabel.setStyle("-fx-text-fill: " + color + ";");
        Label textLabel = new Label(name);
        textLabel.setStyle("-fx-text-fill: " + (lightBg ? "#1a1a1a" : "#e8e8e8") + "; -fx-font-weight: bold; -fx-font-size: 13;");

        HBox content = new HBox(10);
        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(8, 16, 8, 16));
        content.getChildren().addAll(iconLabel, textLabel);

        String bg = lightBg ? "rgba(255,255,255,0.95)" : "rgba(30,30,30,0.95)";
        String bgHover = lightBg ? "rgba(240,240,240,0.98)" : "rgba(45,45,45,0.98)";

        Button btn = new Button();
        btn.setGraphic(content);
        btn.getStyleClass().add("share-platform-btn");
        btn.setStyle("-fx-background-color: " + bg + "; -fx-background-radius: 10; " +
                "-fx-border-color: " + color + "; -fx-border-width: 2; -fx-border-radius: 10; " +
                "-fx-cursor: hand;");
        btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color: " + bgHover + "; -fx-background-radius: 10; " +
                "-fx-border-color: " + color + "; -fx-border-width: 2; -fx-border-radius: 10; -fx-cursor: hand;"));
        btn.setOnMouseExited(e -> btn.setStyle("-fx-background-color: " + bg + "; -fx-background-radius: 10; " +
                "-fx-border-color: " + color + "; -fx-border-width: 2; -fx-border-radius: 10; -fx-cursor: hand;"));
        btn.setOnAction(ev -> action.run());
        return btn;
    }

    @FXML
    private void handleShareFacebook() {
        share("facebook");
    }

    @FXML
    private void handleShareTwitter() {
        share("twitter");
    }

    @FXML
    private void handleShareWhatsApp() {
        share("whatsapp");
    }

    @FXML
    private void handleShareInstagram() {
        share("instagram");
    }

    @FXML
    private void handleShareLinkedIn() {
        share("linkedin");
    }

    private String getShareText() {
        try {
            ForumPost post = postService.getById(postId);
            if (post != null && post.getContent() != null && !post.getContent().isBlank()) {
                String content = post.getContent();
                return content.length() > 500 ? content.substring(0, 497) + "..." : content;
            }
        } catch (SQLException ignored) {}
        return "ForumPost Tabaani #" + postId;
    }

    private void openBrowser(String url) {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(new URI(url));
            }
        } catch (Exception e) {
            showAlert("Erreur", "Impossible d'ouvrir le navigateur: " + e.getMessage());
        }
    }

    private void share(String platform) {
        String text = getShareText();
        String encoded = URLEncoder.encode(text, StandardCharsets.UTF_8);
        String platformName = platform.equals("twitter") ? "X" : platform;

        try {
            switch (platform) {
                case "facebook":
                    openBrowser("https://www.facebook.com/sharer/sharer.php?u=https://tabaani.app&quote=" + encoded);
                    break;
                case "twitter":
                    openBrowser("https://x.com/intent/tweet?text=" + encoded);
                    break;
                case "whatsapp":
                    openBrowser("https://wa.me/?text=" + encoded);
                    break;
                case "instagram":
                    ClipboardContent cc = new ClipboardContent();
                    cc.putString(text);
                    Clipboard.getSystemClipboard().setContent(cc);
                    openBrowser("https://www.instagram.com/");
                    showAlert("Instagram", "Le contenu a été copié. Collez-le dans votre publication Instagram.");
                    break;
                case "linkedin":
                    openBrowser("https://www.linkedin.com/sharing/share-offsite/?url=" + URLEncoder.encode("https://tabaani.app/post/" + postId, StandardCharsets.UTF_8) + "&summary=" + encoded);
                    break;
                default:
                    showAlert("Erreur", "Plateforme non supportée.");
                    return;
            }
            try {
                activityService.addShare(postId, userKey, platform);
            } catch (SQLException e) {
                if (!e.getMessage().contains("Duplicate entry")) {
                    throw e;
                }
            }
            showAlert("Succès", "Ouverture de " + platformName + " pour partager !");
            if (stage != null) stage.close();
        } catch (SQLException e) {
            showAlert("Erreur", "Erreur: " + e.getMessage());
        }
    }

    @FXML
    private void handleCancel() {
        if (stage != null) stage.close();
    }

    private void showAlert(String title, String message) {
        com.esprit.utils.ForumStyledDialog.show(title, message);
    }
}


