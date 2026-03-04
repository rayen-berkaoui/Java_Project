package Controllers;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.beans.binding.Bindings;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.util.Duration;
import org.example.entities.Post;
import org.example.services.ActivityService;
import org.example.services.CommentService;

import java.io.File;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

public class PostCell extends ListCell<Post> {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("d MMM yyyy · HH:mm", Locale.FRENCH);
    private static final int MIN_CARD_WIDTH = 280;

    private final ActivityService activityService;
    private final CommentService commentService;
    private final Consumer<String> onHashtagClick;
    private final List<MediaPlayer> mediaPlayers = new ArrayList<>();

    public PostCell(ActivityService activityService, CommentService commentService) {
        this(activityService, commentService, null);
    }

    public PostCell(ActivityService activityService, CommentService commentService, Consumer<String> onHashtagClick) {
        this.activityService = activityService;
        this.commentService = commentService;
        this.onHashtagClick = onHashtagClick;
    }

    @Override
    protected void updateItem(Post post, boolean empty) {
        super.updateItem(post, empty);

        disposeMediaPlayers();
        if (empty || post == null) {
            setText(null);
            setGraphic(null);
        } else {
            VBox card = buildCard(post);
            if (getListView() != null) {
                card.setMinWidth(MIN_CARD_WIDTH);
                card.setMaxWidth(Double.MAX_VALUE);
                card.prefWidthProperty().bind(getListView().widthProperty().subtract(48));
            }
            setGraphic(card);
        }
    }

    private VBox buildCard(Post post) {
        VBox card = new VBox(18);
        card.getStyleClass().add("post-card");
        HBox header = new HBox(16);
        header.setAlignment(Pos.CENTER_LEFT);

        Label badge = new Label("Post #" + post.getId());
        badge.getStyleClass().add("post-card-badge");

        String dateStr = post.getCreatedAt() != null
                ? post.getCreatedAt().format(DATE_FORMAT)
                : "";
        Label dateLabel = new Label(dateStr);
        dateLabel.getStyleClass().add("post-card-date");

        header.getChildren().addAll(badge, dateLabel);
        card.getChildren().add(header);

        String content = post.getContent() != null ? post.getContent() : "";
        if (!content.isEmpty()) {
            Label contentLabel = new Label(content);
            contentLabel.getStyleClass().add("post-card-content");
            contentLabel.setWrapText(true);
            contentLabel.setMaxWidth(Region.USE_COMPUTED_SIZE);
            contentLabel.maxWidthProperty().bind(Bindings.max(200, card.widthProperty().subtract(56)));
            card.getChildren().add(contentLabel);
        }
        List<String> tags = post.getHashtagList();
        if (!tags.isEmpty()) {
            FlowPane tagsFlow = new FlowPane(8, 6);
            tagsFlow.prefWrapLengthProperty().bind(card.widthProperty().subtract(56));
            for (String tag : tags) {
                Label tagLabel = new Label("#" + tag);
                tagLabel.getStyleClass().add("post-card-hashtag");
                tagLabel.setStyle("-fx-text-fill: #FFD700; -fx-font-size: 12; -fx-underline: false;");
                tagLabel.setCursor(Cursor.HAND);
                tagLabel.setOnMouseEntered(e -> tagLabel.setStyle("-fx-text-fill: #FFE44D; -fx-font-size: 12; -fx-underline: true;"));
                tagLabel.setOnMouseExited(e -> tagLabel.setStyle("-fx-text-fill: #FFD700; -fx-font-size: 12; -fx-underline: false;"));
                if (onHashtagClick != null) {
                    String tagForFilter = tag;
                    tagLabel.setOnMouseClicked(e -> onHashtagClick.accept(tagForFilter));
                }
                tagsFlow.getChildren().add(tagLabel);
            }
            card.getChildren().add(tagsFlow);
        }

        FlowPane mediaFlow = new FlowPane(14, 14);
        mediaFlow.getStyleClass().add("post-card-media-box");
        mediaFlow.prefWrapLengthProperty().bind(card.widthProperty().subtract(40));

        List<String> imagePaths = post.getImagePaths();
        for (String path : imagePaths) {
            VBox imageBox = new VBox(6);
            imageBox.getStyleClass().add("post-card-media-frame");
            imageBox.setAlignment(Pos.CENTER);

            ImageView imageView = new ImageView();
            imageView.setFitWidth(160);
            imageView.setFitHeight(160);
            imageView.setPreserveRatio(true);
            imageView.setSmooth(true);
            loadImage(path, imageView);

            Label imgLabel = new Label("Image");
            imgLabel.getStyleClass().add("post-card-media-label");
            imageBox.getChildren().addAll(imageView, imgLabel);
            mediaFlow.getChildren().add(imageBox);
        }

        List<String> videoPaths = post.getVideoPaths();
        for (String path : videoPaths) {
            VBox videoBox = createVideoPreview(path);
            mediaFlow.getChildren().add(videoBox);
        }

        if (mediaFlow.getChildren().isEmpty() == false) {
            card.getChildren().add(mediaFlow);
        }

        HBox accentLine = new HBox();
        accentLine.getStyleClass().add("accent-line");
        accentLine.setPrefHeight(1);
        accentLine.setMinHeight(1);
        card.getChildren().add(accentLine);

        int likes = getCount(() -> activityService.countLikes(post.getId()));
        int dislikes = getCount(() -> activityService.countDislikes(post.getId()));
        int comments = getCount(() -> commentService.countByPostId(post.getId()));
        int shares = getCount(() -> activityService.countShares(post.getId()));

        HBox statsBox = new HBox(28);
        statsBox.getStyleClass().add("post-card-stats");
        statsBox.setAlignment(Pos.CENTER_LEFT);
        statsBox.setPadding(new Insets(8, 0, 0, 0));

        Label likesLabel = new Label("  👍  " + likes);
        likesLabel.getStyleClass().addAll("post-stat-item", "post-stat-likes");

        Label dislikesLabel = new Label("  👎  " + dislikes);
        dislikesLabel.getStyleClass().addAll("post-stat-item", "post-stat-dislikes");

        Label commentsLabel = new Label("  💬  " + comments);
        commentsLabel.getStyleClass().addAll("post-stat-item", "post-stat-comments");

        Label sharesLabel = new Label("  🔗  " + shares);
        sharesLabel.getStyleClass().addAll("post-stat-item", "post-stat-shares");

        statsBox.getChildren().addAll(likesLabel, dislikesLabel, commentsLabel, sharesLabel);
        card.getChildren().add(statsBox);

        return card;
    }

    private VBox createVideoPreview(String path) {
        VBox videoBox = new VBox(6);
        videoBox.getStyleClass().add("post-card-media-frame");
        videoBox.setMinSize(180, 120);
        videoBox.setAlignment(Pos.CENTER);

        String uri = toFileUri(path);
        if (uri != null) {
            try {
                Media media = new Media(uri);
                MediaPlayer player = new MediaPlayer(media);
                player.setMute(true);
                player.setAutoPlay(false);
                player.setCycleCount(0);
                player.setOnReady(() -> player.seek(Duration.seconds(0.5)));
                player.setOnError(() -> Platform.runLater(() -> showVideoPlaceholder(videoBox)));

                MediaView mediaView = new MediaView(player);
                mediaView.setFitWidth(180);
                mediaView.setFitHeight(120);
                mediaView.setPreserveRatio(true);

                StackPane viewPane = new StackPane(mediaView);
                viewPane.setMinSize(180, 120);
                viewPane.setPrefSize(180, 120);

                Label playOverlay = new Label("▶");
                playOverlay.getStyleClass().add("post-card-media-label");
                playOverlay.setStyle("-fx-font-size: 28; -fx-text-fill: rgba(255,255,255,0.9); -fx-effect: dropshadow(gaussian, black, 8, 0.5, 0, 1);");
                StackPane.setAlignment(playOverlay, Pos.CENTER);
                viewPane.getChildren().add(playOverlay);

                Label videoLabel = new Label("Vidéo");
                videoLabel.getStyleClass().add("post-card-media-label");
                videoBox.getChildren().addAll(viewPane, videoLabel);

                mediaPlayers.add(player);
            } catch (Exception e) {
                showVideoPlaceholder(videoBox);
            }
        } else {
            showVideoPlaceholder(videoBox);
        }
        return videoBox;
    }

    private void showVideoPlaceholder(VBox videoBox) {
        videoBox.getChildren().clear();
        Label videoIcon = new Label("▶");
        videoIcon.getStyleClass().add("post-card-media-label");
        videoIcon.setStyle("-fx-font-size: 36; -fx-text-fill: #FFD700;");
        Label videoLabel = new Label("Vidéo");
        videoLabel.getStyleClass().add("post-card-media-label");
        videoBox.getChildren().addAll(videoIcon, videoLabel);
    }

    private String toFileUri(String path) {
        if (path == null || path.isBlank()) return null;
        path = path.trim();
        if (path.startsWith("file:") || path.startsWith("http")) return path;
        File file = new File(path);
        if (!file.exists()) file = new File(path.replace('/', File.separatorChar).replace('\\', File.separatorChar));
        return file.exists() && file.isFile() ? file.toURI().toString() : null;
    }

    private void disposeMediaPlayers() {
        for (MediaPlayer p : mediaPlayers) {
            try { p.stop(); p.dispose(); } catch (Exception ignored) {}
        }
        mediaPlayers.clear();
    }

    private void loadImage(String path, ImageView imageView) {
        if (path == null || path.isBlank()) return;

        path = path.trim();
        String uri = null;

        try {
            if (path.startsWith("file:") || path.startsWith("http://") || path.startsWith("https://")) {
                uri = path;
            } else {
                File file = new File(path);
                if (!file.exists()) {
                    file = new File(path.replace('/', File.separatorChar).replace('\\', File.separatorChar));
                }
                if (file.exists() && file.isFile()) {
                    uri = file.toURI().toString();
                }
            }
            if (uri != null) {
                Image img = new Image(uri, 160, 160, true, true);
                img.errorProperty().addListener((o, ov, err) -> {
                    if (Boolean.TRUE.equals(err)) imageView.setImage(null);
                });
                imageView.setImage(img);
            }
        } catch (Exception ignored) {
            imageView.setImage(null);
        }
    }

    private int getCount(CountSupplier supplier) {
        try {
            return supplier.get();
        } catch (SQLException e) {
            return 0;
        }
    }

    @FunctionalInterface
    private interface CountSupplier {
        int get() throws SQLException;
    }
}
