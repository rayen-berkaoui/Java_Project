package com.esprit.controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.stage.Stage;
import javafx.util.Duration;
import com.esprit.entities.ForumComment;
import com.esprit.entities.ForumPost;
import com.esprit.entities.ForumReactionType;
import com.esprit.services.*;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

public class ForumDetailsController implements Initializable {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("d MMM yyyy · HH:mm", Locale.FRENCH);

    @FXML
    private VBox postContentContainer;

    @FXML
    private HBox actionButtonsBar;

    @FXML
    private Button likeBtn;

    @FXML
    private Button dislikeBtn;

    @FXML
    private Button shareBtn;

    @FXML
    private Button modifyBtn;

    @FXML
    private Button deleteBtn;

    @FXML
    private TextArea commentInput;

    @FXML
    private ListView<ForumComment> commentsListView;

    private int postId;
    private String currentUser = "test_user";
    private ForumAppNavigator navigator;

    private final ForumPostService postService = new ForumPostService();
    private final ForumCommentService commentService = new ForumCommentService();
    private final ForumActivityService activityService = new ForumActivityService();
    private final ForumContentModerationService moderationService = new ForumContentModerationService();
    private final ForumShareService shareService = new ForumShareService();

    private final List<MediaPlayer> videoPlayers = new ArrayList<>();
    private Runnable onRefreshCallback;

    public void setNavigator(ForumAppNavigator navigator) {
        this.navigator = navigator;
    }

    public void setRefreshCallback(Runnable callback) {
        this.onRefreshCallback = callback;
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        commentsListView.setCellFactory(listView -> new ForumCommentCell(this, currentUser));
        styleActionButtons();
    }

    private void styleActionButtons() {
        String base = "-fx-font-size: 13; -fx-font-weight: bold; -fx-padding: 10 26; "
                + "-fx-background-radius: 22; -fx-border-radius: 22; -fx-cursor: hand; "
                + "-fx-background-insets: 0; -fx-border-width: 1.5; ";

        String likeStyle = base
                + "-fx-background-color: rgba(81,207,102,0.12); -fx-text-fill: #51CF66; "
                + "-fx-border-color: rgba(81,207,102,0.35); "
                + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 6, 0, 0, 2);";
        String likeHover = base
                + "-fx-background-color: rgba(81,207,102,0.30); -fx-text-fill: #51CF66; "
                + "-fx-border-color: #51CF66; "
                + "-fx-effect: dropshadow(gaussian, rgba(81,207,102,0.3), 14, 0, 0, 0);";

        String dislikeStyle = base
                + "-fx-background-color: rgba(255,107,107,0.12); -fx-text-fill: #FF6B6B; "
                + "-fx-border-color: rgba(255,107,107,0.35); "
                + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 6, 0, 0, 2);";
        String dislikeHover = base
                + "-fx-background-color: rgba(255,107,107,0.30); -fx-text-fill: #FF6B6B; "
                + "-fx-border-color: #FF6B6B; "
                + "-fx-effect: dropshadow(gaussian, rgba(255,107,107,0.3), 14, 0, 0, 0);";

        String shareStyle = base
                + "-fx-background-color: rgba(255,215,0,0.10); -fx-text-fill: #FFD700; "
                + "-fx-border-color: rgba(255,215,0,0.30); "
                + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 6, 0, 0, 2);";
        String shareHover = base
                + "-fx-background-color: rgba(255,215,0,0.25); -fx-text-fill: #FFD700; "
                + "-fx-border-color: #FFD700; "
                + "-fx-effect: dropshadow(gaussian, rgba(255,215,0,0.25), 14, 0, 0, 0);";

        applyBtnStyle(likeBtn, likeStyle, likeHover);
        applyBtnStyle(dislikeBtn, dislikeStyle, dislikeHover);
        applyBtnStyle(shareBtn, shareStyle, shareHover);
    }

    private void applyBtnStyle(Button btn, String normal, String hover) {
        if (btn == null) return;
        btn.setStyle(normal);
        btn.setOnMouseEntered(e -> btn.setStyle(hover));
        btn.setOnMouseExited(e -> btn.setStyle(normal));
        btn.setOnMousePressed(e -> btn.setStyle(hover + "-fx-scale-x: 0.95; -fx-scale-y: 0.95;"));
        btn.setOnMouseReleased(e -> btn.setStyle(hover));
    }

    public void setPostId(int postId) {
        this.postId = postId;
        loadPostDetails();
        loadComments();
        Platform.runLater(() -> {
            if (postContentContainer != null && postContentContainer.getScene() != null && postContentContainer.getScene().getWindow() != null) {
                postContentContainer.getScene().getWindow().setOnCloseRequest(e -> disposeVideoPlayers());
            }
        });
    }

    private void loadPostDetails() {
        try {
            ForumPost post = postService.getById(postId);
            if (post == null) {
                showAlert("Erreur", "ForumPost introuvable.");
                return;
            }

            disposeVideoPlayers();
            postContentContainer.getChildren().clear();

            HBox header = new HBox(12);
            header.setAlignment(Pos.CENTER_LEFT);

            Label badge = new Label("ForumPost #" + post.getId());
            badge.getStyleClass().add("post-card-badge");

            String dateStr = post.getCreatedAt() != null ? post.getCreatedAt().format(DATE_FORMAT) : "";
            Label dateLabel = new Label(dateStr);
            dateLabel.getStyleClass().add("post-card-date");

            header.getChildren().addAll(badge, dateLabel);
            postContentContainer.getChildren().add(header);

            String content = post.getContent() != null ? post.getContent() : "";
            if (!content.isEmpty()) {
                Label contentLabel = new Label(content);
                contentLabel.getStyleClass().add("post-card-content");
                contentLabel.setWrapText(true);
                contentLabel.setMaxWidth(800);
                postContentContainer.getChildren().add(contentLabel);
            }

            List<String> tags = post.getHashtagList();
            if (!tags.isEmpty()) {
                FlowPane tagsFlow = new FlowPane(6, 4);
                tagsFlow.setPrefWrapLength(800);
                for (String tag : tags) {
                    Label tagLabel = new Label("#" + tag);

                    tagLabel.setStyle("-fx-text-fill: #FFD700; -fx-font-size: 13;");
                    tagsFlow.getChildren().add(tagLabel);
                }
                postContentContainer.getChildren().add(tagsFlow);
            }
            List<String> imagePaths = post.getImagePaths();
            if (!imagePaths.isEmpty()) {
                FlowPane imagesFlow = new FlowPane(12, 12);
                imagesFlow.setPrefWrapLength(820);
                for (String path : imagePaths) {
                    VBox imgBox = new VBox(6);
                    imgBox.getStyleClass().add("post-card-media-frame");
                    imgBox.setAlignment(Pos.CENTER);

                    ImageView iv = new ImageView();
                    iv.setFitWidth(240);
                    iv.setFitHeight(240);
                    iv.setPreserveRatio(true);
                    iv.setSmooth(true);
                    loadImage(path, iv);

                    imgBox.getChildren().add(iv);
                    imagesFlow.getChildren().add(imgBox);
                }
                postContentContainer.getChildren().add(imagesFlow);
            }

            List<String> videoPaths = post.getVideoPaths();
            if (!videoPaths.isEmpty()) {
                VBox videosBox = new VBox(16);
                for (String path : videoPaths) {
                    VBox videoCard = createVideoPlayer(path);
                    videosBox.getChildren().add(videoCard);
                }
                postContentContainer.getChildren().add(videosBox);
            }

            updateReactionCounts();
        } catch (SQLException e) {
            showAlert("Erreur", "Impossible de charger le post: " + e.getMessage());
        }
    }

    private VBox createVideoPlayer(String path) {
        VBox videoCard = new VBox(8);
        videoCard.getStyleClass().add("details-video-card");

        String uri = toFileUri(path);
        if (uri != null) {
            try {
                Media media = new Media(uri);
                MediaPlayer player = new MediaPlayer(media);
                player.setMute(false);
                player.setAutoPlay(false);

                MediaView mediaView = new MediaView(player);
                mediaView.setFitWidth(560);
                mediaView.setFitHeight(315);
                mediaView.setPreserveRatio(true);

                StackPane videoPane = new StackPane(mediaView);
                videoPane.setMinSize(560, 315);
                videoPane.setPrefSize(560, 315);
                videoPane.getStyleClass().add("post-card-media-frame");

                Button playBtn = new Button("▶ Lire");
                playBtn.getStyleClass().add("dashboard-button");
                playBtn.setOnAction(e -> {
                    if (player.getStatus() == MediaPlayer.Status.PLAYING) {
                        player.pause();
                        playBtn.setText("▶ Lire");
                    } else {
                        player.play();
                        playBtn.setText("⏸ Pause");
                    }
                });

                player.setOnEndOfMedia(() -> Platform.runLater(() -> {
                    playBtn.setText("▶ Lire");
                    player.seek(Duration.ZERO);
                }));

                videoCard.getChildren().addAll(videoPane, playBtn);
                videoPlayers.add(player);
            } catch (Exception e) {
                Label err = new Label("Impossible de charger la vidéo");
                err.getStyleClass().add("post-card-date");
                videoCard.getChildren().add(err);
            }
        }
        return videoCard;
    }

    private String toFileUri(String path) {
        if (path == null || path.isBlank()) return null;
        path = path.trim();
        if (path.startsWith("file:") || path.startsWith("http")) return path;
        File file = new File(path);
        if (!file.exists()) file = new File(path.replace('/', File.separatorChar).replace('\\', File.separatorChar));
        return file.exists() && file.isFile() ? file.toURI().toString() : null;
    }

    private void loadImage(String path, ImageView iv) {
        String uri = toFileUri(path);
        if (uri != null) {
            Image img = new Image(uri, 240, 240, true, true);
            iv.setImage(img);
        }
    }

    private void disposeVideoPlayers() {
        for (MediaPlayer p : videoPlayers) {
            try { p.stop(); p.dispose(); } catch (Exception ignored) {}
        }
        videoPlayers.clear();
    }

    private void updateReactionCounts() {
        try {
            int likes = activityService.countLikes(postId);
            int dislikes = activityService.countDislikes(postId);
            int shares = activityService.countShares(postId);
            int commentsCount = commentService.countByPostId(postId);

            likeBtn.setText("👍 " + likes);
            dislikeBtn.setText("👎 " + dislikes);
            shareBtn.setText("📤 Partager (" + shares + ")");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void loadComments() {
        try {
            commentsListView.getItems().setAll(commentService.afficherByPost(postId));
            updateReactionCounts();
        } catch (SQLException e) {
            showAlert("Erreur", "Impossible de charger les commentaires: " + e.getMessage());
        }
    }

    @FXML
    private void handleLike() {
        try {
            activityService.toggleReaction(postId, currentUser, ForumReactionType.LIKE);
            updateReactionCounts();
        } catch (SQLException e) {
            showAlert("Erreur", "Impossible d'aimer le post: " + e.getMessage());
        }
    }

    @FXML
    private void handleDislike() {
        try {
            activityService.toggleReaction(postId, currentUser, ForumReactionType.DISLIKE);
            updateReactionCounts();
        } catch (SQLException e) {
            showAlert("Erreur", "Impossible de disliker le post: " + e.getMessage());
        }
    }

    @FXML
    private void handleShare() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/forum/Share.fxml"));
            Parent root = loader.load();
            ForumShareController controller = loader.getController();
            controller.setPostId(postId);
            Stage stage = new Stage();
            stage.setTitle("Partager");
            stage.setScene(new Scene(root));
            controller.setStage(stage);
            stage.showAndWait();
            updateReactionCounts();
        } catch (IOException e) {
            showAlert("Erreur", "Impossible d'ouvrir le formulaire: " + e.getMessage());
        }
    }

    @FXML
    private void handleModifyPost() {
        try {
            ForumPost post = postService.getById(postId);
            if (post == null) {
                showAlert("Erreur", "ForumPost introuvable.");
                return;
            }
            if (navigator != null) navigator.showAddPost(post);
        } catch (SQLException e) {
            showAlert("Erreur", e.getMessage());
        }
    }

    @FXML
    private void handleDeletePost() {
        if (com.esprit.utils.ForumStyledDialog.confirm("Supprimer ce post ?",
                "Cette action est irréversible. Tous les commentaires et réactions seront également supprimés.")) {
            try {
                postService.supprimer(postId);
                if (onRefreshCallback != null) onRefreshCallback.run();
                disposeVideoPlayers();
                if (navigator != null) navigator.goBack();
            } catch (SQLException e) {
                showAlert("Erreur", "Impossible de supprimer: " + e.getMessage());
            }
        }
    }

    @FXML
    private void handleAddComment() {
        String content = commentInput.getText().trim();
        if (content.isEmpty()) {
            showAlert("Erreur", "Le commentaire ne peut pas être vide");
            return;
        }
        ForumContentModerationService.ModerationResult result = moderationService.checkPost(content);
        if (!result.isAllowed()) {
            showAlert("Contenu non autorisé", result.getRejectionReason());
            return;
        }
        try {
            ForumComment comment = new ForumComment();
            comment.setPostId(postId);
            comment.setUserKey(currentUser);
            comment.setContent(content);
            comment.setCreatedAt(LocalDateTime.now());
            commentService.ajouter(comment);
            commentInput.clear();
            loadComments();
        } catch (SQLException e) {
            showAlert("Erreur", "Impossible d'ajouter le commentaire: " + e.getMessage());
        }
    }

    public void handleCommentLike(int commentId) {
        try {
            activityService.toggleCommentReaction(commentId, postId, currentUser, ForumReactionType.LIKE);
            loadComments();
        } catch (SQLException e) {
            showAlert("Erreur", "Impossible d'aimer le commentaire: " + e.getMessage());
        }
    }

    public void handleCommentDislike(int commentId) {
        try {
            activityService.toggleCommentReaction(commentId, postId, currentUser, ForumReactionType.DISLIKE);
            loadComments();
        } catch (SQLException e) {
            showAlert("Erreur", "Impossible de disliker le commentaire: " + e.getMessage());
        }
    }

    public void handleEditComment(ForumComment comment) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/forum/ForumEditCommentController.fxml"));
            Parent root = loader.load();
            ForumEditCommentController controller = loader.getController();
            controller.setComment(comment);
            Stage stage = new Stage();
            stage.setTitle("Modifier commentaire");
            stage.setScene(new Scene(root));
            stage.showAndWait();
            loadComments();
        } catch (IOException e) {
            showAlert("Erreur", "Impossible d'ouvrir l'éditeur: " + e.getMessage());
        }
    }

    public void handleDeleteComment(int commentId) {
        if (com.esprit.utils.ForumStyledDialog.confirm("Supprimer commentaire",
                "Voulez-vous vraiment supprimer ce commentaire?")) {
            try {
                commentService.supprimer(commentId);
                loadComments();
            } catch (SQLException e) {
                showAlert("Erreur", "Impossible de supprimer: " + e.getMessage());
            }
        }
    }

    private void showAlert(String title, String message) {
        com.esprit.utils.ForumStyledDialog.show(title, message);
    }
}


