package Controllers;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Region;
import javafx.geometry.Pos;
import javafx.animation.Timeline;
import javafx.animation.KeyFrame;
import javafx.util.Duration;
import javafx.scene.layout.Priority;
import org.example.entities.Comment;
import org.example.entities.Post;
import org.example.services.AdminService;
import org.example.services.ChatModerationService;
import org.example.services.DashboardStatsService;
import org.example.services.PostService;
import org.example.chat.ChatClient;
import org.example.chat.ChatWebSocketServer;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class AdminDashboardController implements Initializable {

    private static final DateTimeFormatter DF = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final int PREVIEW_LEN = 80;

    @FXML private Label lblPostsCount;
    @FXML private Label lblCommentsCount;
    @FXML private Label lblHashtagsCount;
    @FXML private Label lblSharesCount;
    @FXML private Label lblReportsCount;
    @FXML private Label lblBlockedCount;
    @FXML private Label lblLastRefresh;

    @FXML private LineChart<String, Number> chartPostsPerDay;
    @FXML private PieChart chartReactions;
    @FXML private BarChart<String, Number> chartTopHashtags;
    @FXML private BarChart<String, Number> chartTopCommenters;
    @FXML private PieChart chartReports;
    @FXML private PieChart chartWarnings;
    @FXML private BarChart<String, Number> chartByHour;
    @FXML private BarChart<String, Number> chartTopPosts;

    @FXML private TabPane tabPane;
    @FXML private TableView<Post> postsTable;
    @FXML private TableColumn<Post, Number> colPostId;
    @FXML private TableColumn<Post, String> colPostContent;
    @FXML private TableColumn<Post, String> colPostDate;
    @FXML private TableColumn<Post, Void> colPostAction;
    @FXML private TableView<Comment> commentsTable;
    @FXML private TableColumn<Comment, Number> colCommentId;
    @FXML private TableColumn<Comment, Number> colCommentPostId;
    @FXML private TableColumn<Comment, String> colCommentContent;
    @FXML private TableColumn<Comment, String> colCommentUser;
    @FXML private TableColumn<Comment, String> colCommentDate;
    @FXML private TableColumn<Comment, Void> colCommentAction;
    @FXML private TableView<Map.Entry<String, Integer>> hashtagsTable;
    @FXML private TableColumn<Map.Entry<String, Integer>, String> colHashtagName;
    @FXML private TableColumn<Map.Entry<String, Integer>, Number> colHashtagCount;
    @FXML private TableColumn<Map.Entry<String, Integer>, Void> colHashtagAction;
    @FXML private TextField newHashtagField;

    @FXML private Label lblPendingAlerts;
    @FXML private TableView<ChatModerationService.ChatViolation> violationsTable;
    @FXML private TableColumn<ChatModerationService.ChatViolation, Number> colViolId;
    @FXML private TableColumn<ChatModerationService.ChatViolation, String> colViolUser;
    @FXML private TableColumn<ChatModerationService.ChatViolation, String> colViolBadWord;
    @FXML private TableColumn<ChatModerationService.ChatViolation, String> colViolStatus;
    @FXML private TableColumn<ChatModerationService.ChatViolation, Void> colViolAction;

    @FXML private VBox chatFeedContainer;
    @FXML private ScrollPane chatFeedScroll;
    @FXML private Label lblWsStatus;
    @FXML private Label lblMsgStats;
    @FXML private Label lblOnlineStats;
    @FXML private TextField txtChatSearch;

    @FXML private Label lblConnectedCount;
    @FXML private VBox adminConnectedUsersList;
    @FXML private TextField adminMessageField;

    @FXML private Label lblBanCount;
    @FXML private TableView<ChatModerationService.ChatBan> bannedUsersTable;
    @FXML private TableColumn<ChatModerationService.ChatBan, Number> colBanId;
    @FXML private TableColumn<ChatModerationService.ChatBan, String> colBanUser;
    @FXML private TableColumn<ChatModerationService.ChatBan, String> colBanReason;
    @FXML private TableColumn<ChatModerationService.ChatBan, String> colBanDate;
    @FXML private TableColumn<ChatModerationService.ChatBan, String> colBanUntil;
    @FXML private TableColumn<ChatModerationService.ChatBan, String> colBanRemaining;
    @FXML private TableColumn<ChatModerationService.ChatBan, String> colBanStatus;
    @FXML private TableColumn<ChatModerationService.ChatBan, Void> colBanAction;

    private ChatClient adminChatClient;
    private volatile boolean adminWsConnected = false;
    private Timeline chatAutoRefresh;

    private AppNavigator navigator;
    private final AdminService adminService = new AdminService();
    private final PostService postService = new PostService();
    private final DashboardStatsService statsService = new DashboardStatsService();
    private final ChatModerationService chatModerationService = new ChatModerationService();

    public void setNavigator(AppNavigator navigator) {
        this.navigator = navigator;
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        initPostsColumns();
        initCommentsColumns();
        initHashtagsColumns();
        initViolationsColumns();
        initBannedUsersColumns();
        initChatAutoRefresh();
        connectAdminWebSocket();
        refreshAll();
    }

    @FXML
    private void refreshAll() {
        refreshKPIs();
        refreshCharts();
        refreshTables();
        refreshChatAlerts();
        refreshChatMessages();
        refreshBannedUsers();
        if (lblLastRefresh != null) {
            lblLastRefresh.setText("Dernière MAJ : " + LocalDateTime.now().format(DF));
        }
    }

    private void refreshKPIs() {
        lblPostsCount.setText(String.valueOf(statsService.countPosts()));
        lblCommentsCount.setText(String.valueOf(statsService.countComments()));
        if (lblSharesCount != null) lblSharesCount.setText(String.valueOf(statsService.countShares()));
        if (lblReportsCount != null) lblReportsCount.setText(String.valueOf(statsService.countReports()));
        if (lblBlockedCount != null) lblBlockedCount.setText(String.valueOf(statsService.countBlockedUsers()));
        try {
            int hashtagCount = postService.getHashtagsWithCount().size();
            lblHashtagsCount.setText(String.valueOf(hashtagCount));
        } catch (SQLException e) {
            lblHashtagsCount.setText("?");
        }
    }

    private void refreshCharts() {
        refreshPostsPerDayChart();
        refreshReactionsChart();
        refreshTopHashtagsChart();
        refreshTopCommentersChart();
        refreshReportsChart();
        refreshWarningsChart();
        refreshByHourChart();
        refreshTopPostsChart();
    }

    private void refreshPostsPerDayChart() {
        if (chartPostsPerDay == null) return;
        chartPostsPerDay.getData().clear();

        Map<String, Integer> postsPerDay = statsService.getPostsPerDay(30);
        Map<String, Integer> commentsPerDay = statsService.getCommentsPerDay(30);

        XYChart.Series<String, Number> postsSeries = new XYChart.Series<>();
        postsSeries.setName("Publications");
        XYChart.Series<String, Number> commentsSeries = new XYChart.Series<>();
        commentsSeries.setName("Commentaires");

        int i = 0;
        for (Map.Entry<String, Integer> entry : postsPerDay.entrySet()) {
            String label = (i % 3 == 0) ? entry.getKey().substring(5) : "";
            postsSeries.getData().add(new XYChart.Data<>(entry.getKey().substring(5), entry.getValue()));
            i++;
        }
        for (Map.Entry<String, Integer> entry : commentsPerDay.entrySet()) {
            commentsSeries.getData().add(new XYChart.Data<>(entry.getKey().substring(5), entry.getValue()));
        }

        chartPostsPerDay.getData().addAll(postsSeries, commentsSeries);
    }

    private void refreshReactionsChart() {
        if (chartReactions == null) return;
        Map<String, Integer> reactions = statsService.getReactionCounts();
        chartReactions.setData(FXCollections.observableArrayList(
                new PieChart.Data("👍 Like (" + reactions.getOrDefault("LIKE", 0) + ")", reactions.getOrDefault("LIKE", 0)),
                new PieChart.Data("👎 Dislike (" + reactions.getOrDefault("DISLIKE", 0) + ")", reactions.getOrDefault("DISLIKE", 0)),
                new PieChart.Data("🔗 Share (" + reactions.getOrDefault("SHARE", 0) + ")", reactions.getOrDefault("SHARE", 0))
        ));
    }

    private void refreshTopHashtagsChart() {
        if (chartTopHashtags == null) return;
        chartTopHashtags.getData().clear();
        Map<String, Integer> top = statsService.getTopHashtags(10);
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Hashtags");
        for (Map.Entry<String, Integer> e : top.entrySet()) {
            series.getData().add(new XYChart.Data<>("#" + e.getKey(), e.getValue()));
        }
        chartTopHashtags.getData().add(series);
    }

    private void refreshTopCommentersChart() {
        if (chartTopCommenters == null) return;
        chartTopCommenters.getData().clear();
        Map<String, Integer> top = statsService.getTopCommenters(5);
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Commentaires");
        for (Map.Entry<String, Integer> e : top.entrySet()) {
            series.getData().add(new XYChart.Data<>(e.getKey(), e.getValue()));
        }
        chartTopCommenters.getData().add(series);
    }

    private void refreshReportsChart() {
        if (chartReports == null) return;
        Map<String, Integer> reports = statsService.getReportsByStatus();
        chartReports.setData(FXCollections.observableArrayList(
                new PieChart.Data("En attente (" + reports.getOrDefault("PENDING", 0) + ")", reports.getOrDefault("PENDING", 0)),
                new PieChart.Data("Résolu (" + reports.getOrDefault("RESOLVED", 0) + ")", reports.getOrDefault("RESOLVED", 0)),
                new PieChart.Data("Rejeté (" + reports.getOrDefault("DISMISSED", 0) + ")", reports.getOrDefault("DISMISSED", 0))
        ));
    }

    private void refreshWarningsChart() {
        if (chartWarnings == null) return;
        Map<String, Integer> warnings = statsService.getWarningsByLevel();
        chartWarnings.setData(FXCollections.observableArrayList(
                new PieChart.Data("Alerte 1 (" + warnings.getOrDefault("Alerte 1", 0) + ")", warnings.getOrDefault("Alerte 1", 0)),
                new PieChart.Data("Alerte 2 (" + warnings.getOrDefault("Alerte 2", 0) + ")", warnings.getOrDefault("Alerte 2", 0)),
                new PieChart.Data("Bloqué (" + warnings.getOrDefault("Bloqué", 0) + ")", warnings.getOrDefault("Bloqué", 0))
        ));
    }

    private void refreshByHourChart() {
        if (chartByHour == null) return;
        chartByHour.getData().clear();
        Map<String, Integer> byHour = statsService.getPostsByHour();
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Posts");
        for (Map.Entry<String, Integer> e : byHour.entrySet()) {
            series.getData().add(new XYChart.Data<>(e.getKey(), e.getValue()));
        }
        chartByHour.getData().add(series);
    }

    private void refreshTopPostsChart() {
        if (chartTopPosts == null) return;
        chartTopPosts.getData().clear();
        Map<String, Integer> top = statsService.getTopPostsByComments(7);
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Commentaires");
        for (Map.Entry<String, Integer> e : top.entrySet()) {
            series.getData().add(new XYChart.Data<>(e.getKey(), e.getValue()));
        }
        chartTopPosts.getData().add(series);
    }

    private void refreshTables() {
        try {
            List<Post> posts = adminService.getAllPosts();
            postsTable.getItems().setAll(posts);
        } catch (SQLException e) {
            postsTable.getItems().clear();
        }
        try {
            List<Comment> comments = adminService.getAllComments();
            commentsTable.getItems().setAll(comments);
        } catch (SQLException e) {
            commentsTable.getItems().clear();
        }
        try {
            List<Map.Entry<String, Integer>> tags = postService.getHashtagsWithCount();
            hashtagsTable.getItems().setAll(tags);
        } catch (SQLException e) {
            hashtagsTable.getItems().clear();
        }
    }

    private void initPostsColumns() {
        colPostId.setCellValueFactory(c -> new SimpleIntegerProperty(c.getValue().getId()));
        colPostContent.setCellValueFactory(c -> {
            String s = c.getValue().getContent();
            if (s == null) s = "";
            if (s.length() > PREVIEW_LEN) s = s.substring(0, PREVIEW_LEN) + "...";
            return new SimpleStringProperty(s);
        });
        colPostDate.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getCreatedAt() != null ? c.getValue().getCreatedAt().format(DF) : ""));
        colPostAction.setCellFactory(col -> {
            TableCell<Post, Void> cell = new TableCell<>() {
                final Button btn = new Button("Supprimer");
                { btn.getStyleClass().add("dashboard-button-danger"); }
                @Override
                protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                        setGraphic(null);
                    } else {
                        Post p = getTableRow().getItem();
                        btn.setOnAction(e -> deletePost(p.getId()));
                        setGraphic(btn);
                    }
                }
            };
            return cell;
        });
    }

    private void initCommentsColumns() {
        colCommentId.setCellValueFactory(c -> new SimpleIntegerProperty(c.getValue().getId()));
        colCommentPostId.setCellValueFactory(c -> new SimpleIntegerProperty(c.getValue().getPostId()));
        colCommentContent.setCellValueFactory(c -> {
            String s = c.getValue().getContent();
            if (s == null) s = "";
            if (s.length() > PREVIEW_LEN) s = s.substring(0, PREVIEW_LEN) + "...";
            return new SimpleStringProperty(s);
        });
        colCommentUser.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getUserKey() != null ? c.getValue().getUserKey() : ""));
        colCommentDate.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getCreatedAt() != null ? c.getValue().getCreatedAt().format(DF) : ""));
        colCommentAction.setCellFactory(col -> {
            TableCell<Comment, Void> cell = new TableCell<>() {
                final Button btn = new Button("Supprimer");
                { btn.getStyleClass().add("dashboard-button-danger"); }
                @Override
                protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                        setGraphic(null);
                    } else {
                        Comment c = getTableRow().getItem();
                        btn.setOnAction(e -> deleteComment(c.getId()));
                        setGraphic(btn);
                    }
                }
            };
            return cell;
        });
    }

    private void initHashtagsColumns() {
        colHashtagName.setCellValueFactory(c -> new SimpleStringProperty("#" + (c.getValue() != null ? c.getValue().getKey() : "")));
        colHashtagCount.setCellValueFactory(c -> new SimpleIntegerProperty(c.getValue() != null ? c.getValue().getValue() : 0));
        colHashtagAction.setCellFactory(col -> {
            TableCell<Map.Entry<String, Integer>, Void> cell = new TableCell<>() {
                final Button btn = new Button("Supprimer");
                { btn.getStyleClass().add("dashboard-button-danger"); }
                @Override
                protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                        setGraphic(null);
                    } else {
                        Map.Entry<String, Integer> entry = getTableRow().getItem();
                        String tag = entry.getKey();
                        int count = entry.getValue() != null ? entry.getValue() : 0;
                        btn.setOnAction(e -> deleteHashtag(tag, count));
                        setGraphic(btn);
                    }
                }
            };
            return cell;
        });
    }

    private void deletePost(int postId) {
        if (!confirm("Supprimer la publication #" + postId + " ?")) return;
        try {
            adminService.deletePost(postId);
            showAlert("Succès", "Publication supprimée.");
            refreshAll();
        } catch (SQLException e) {
            showAlert("Erreur", e.getMessage());
        }
    }

    private void deleteComment(int commentId) {
        if (!confirm("Supprimer le commentaire #" + commentId + " ?")) return;
        try {
            adminService.deleteComment(commentId);
            showAlert("Succès", "Commentaire supprimé.");
            refreshAll();
        } catch (SQLException e) {
            showAlert("Erreur", e.getMessage());
        }
    }

    private void deleteHashtag(String tag, int count) {
        String msg = count > 0
                ? "Supprimer le hashtag #" + tag + " de tous les posts (" + count + " occurrence(s)) ?"
                : "Supprimer le hashtag #" + tag + " de la liste recommandée ?";
        if (!confirm(msg)) return;
        try {
            if (count > 0) {
                postService.removeHashtagFromAllPosts(tag);
            } else {
                postService.removeRecommendedHashtag(tag);
            }
            showAlert("Succès", "Hashtag supprimé.");
            refreshAll();
        } catch (SQLException e) {
            showAlert("Erreur", e.getMessage());
        } catch (IOException e) {
            showAlert("Erreur", e.getMessage());
        }
    }

    @FXML
    private void handleAddHashtag() {
        if (newHashtagField == null) return;
        String tag = newHashtagField.getText() != null ? newHashtagField.getText().trim() : "";
        if (tag.isEmpty()) {
            showAlert("Attention", "Saisissez un hashtag.");
            return;
        }
        try {
            postService.addRecommendedHashtag(tag);
            newHashtagField.clear();
            showAlert("Succès", "Hashtag ajouté.");
            refreshAll();
        } catch (IOException e) {
            showAlert("Erreur", "Impossible d'écrire le fichier : " + e.getMessage());
        }
    }

    @FXML
    private void handleRetour() {
        if (navigator != null) navigator.showTabaaniConnect();
    }

    private void initViolationsColumns() {
        if (violationsTable == null) return;
        colViolId.setCellValueFactory(c -> new SimpleIntegerProperty(c.getValue().getId()));
        colViolUser.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getUsername()));
        colViolBadWord.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getBadWord() != null ? c.getValue().getBadWord() : ""));
        colViolStatus.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getStatus()));
        colViolStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) { setText(null); setStyle(""); return; }
                setText(status);
                switch (status) {
                    case "PENDING" -> setStyle("-fx-text-fill: #FFB347; -fx-font-weight: bold;");
                    case "BANNED" -> setStyle("-fx-text-fill: #FF4444; -fx-font-weight: bold;");
                    case "RESOLVED" -> setStyle("-fx-text-fill: #4CAF50; -fx-font-weight: bold;");
                    default -> setStyle("");
                }
            }
        });
        colViolAction.setCellFactory(col -> new TableCell<>() {
            final Button banBtn = new Button("\uD83D\uDEAB");
            final Button resolveBtn = new Button("\u2705");
            final javafx.scene.layout.HBox box = new javafx.scene.layout.HBox(3, banBtn, resolveBtn);
            {
                banBtn.getStyleClass().add("dashboard-button-danger");
                banBtn.setStyle("-fx-font-size: 10; -fx-padding: 2 6;");
                banBtn.setTooltip(new javafx.scene.control.Tooltip("Bannir"));
                resolveBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-size: 10; -fx-padding: 2 6; -fx-cursor: hand; -fx-background-radius: 4;");
                resolveBtn.setTooltip(new javafx.scene.control.Tooltip("R\u00e9soudre"));
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                } else {
                    ChatModerationService.ChatViolation v = getTableRow().getItem();
                    boolean isPending = "PENDING".equals(v.getStatus());
                    banBtn.setDisable(!isPending);
                    resolveBtn.setDisable(!isPending);
                    banBtn.setOnAction(e -> banChatUser(v));
                    resolveBtn.setOnAction(e -> resolveViolation(v));
                    setGraphic(box);
                }
            }
        });
    }

    @FXML
    private void refreshChatAlerts() {
        if (violationsTable == null) return;
        List<ChatModerationService.ChatViolation> violations = chatModerationService.getAllViolations();
        violationsTable.getItems().setAll(violations);
        int pending = chatModerationService.countPendingViolations();
        if (lblPendingAlerts != null) {
            lblPendingAlerts.setText("⚠️ " + pending + " alerte" + (pending != 1 ? "s" : ""));
            lblPendingAlerts.setStyle(pending > 0
                    ? "-fx-text-fill: #FF4444; -fx-font-size: 11; -fx-font-weight: bold;"
                    : "-fx-text-fill: #4CAF50; -fx-font-size: 11;");
        }
    }

    private void banChatUser(ChatModerationService.ChatViolation violation) {
        org.example.utils.StyledDialog.textInput(
                "Bannir l'utilisateur",
                "Bannir \"" + violation.getUsername() + "\" du chat",
                "Durée du ban (heures) :",
                "24"
        ).ifPresent(hoursStr -> {
            try {
                int hours = Integer.parseInt(hoursStr.trim());
                if (hours <= 0) { showAlert("Erreur", "La dur\u00e9e doit \u00eatre positive."); return; }
                chatModerationService.banUser(violation.getUsername(), hours,
                        "Mot interdit d\u00e9tect\u00e9 : " + violation.getBadWord());
                showAlert("Succ\u00e8s", "\"" + violation.getUsername() + "\" a \u00e9t\u00e9 banni du chat pour " + hours + "h.");
                refreshChatAlerts();
            } catch (NumberFormatException e) {
                showAlert("Erreur", "Veuillez entrer un nombre valide.");
            } catch (java.sql.SQLException e) {
                showAlert("Erreur", "Erreur lors du bannissement : " + e.getMessage());
            }
        });
    }

    private void resolveViolation(ChatModerationService.ChatViolation violation) {
        try {
            chatModerationService.markResolved(violation.getId());
            refreshChatAlerts();
        } catch (java.sql.SQLException e) {
            showAlert("Erreur", "Erreur : " + e.getMessage());
        }
    }

    private void initBannedUsersColumns() {
        if (bannedUsersTable == null) return;
        colBanId.setCellValueFactory(c -> new SimpleIntegerProperty(c.getValue().getId()));
        colBanUser.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getUsername()));
        colBanReason.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getReason() != null ? c.getValue().getReason() : ""));
        colBanDate.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getFormattedBannedAt()));
        colBanUntil.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getFormattedBannedUntil()));
        colBanRemaining.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getRemainingTime()));
        colBanRemaining.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String remaining, boolean empty) {
                super.updateItem(remaining, empty);
                if (empty || remaining == null) { setText(null); setStyle(""); return; }
                setText(remaining);
                if ("Expiré".equals(remaining)) {
                    setStyle("-fx-text-fill: #666; -fx-font-style: italic;");
                } else {
                    setStyle("-fx-text-fill: #FF6B6B; -fx-font-weight: bold;");
                }
            }
        });
        colBanStatus.setCellValueFactory(c -> {
            boolean active = c.getValue().isActive();
            return new SimpleStringProperty(active ? "ACTIF" : "EXPIRÉ");
        });
        colBanStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) { setText(null); setStyle(""); return; }
                setText(status);
                if ("ACTIF".equals(status)) {
                    setStyle("-fx-text-fill: #FF4444; -fx-font-weight: bold;");
                } else {
                    setStyle("-fx-text-fill: #666; -fx-font-style: italic;");
                }
            }
        });
        colBanAction.setCellFactory(col -> new TableCell<>() {
            final Button unbanBtn = new Button("🔓 Débloquer");
            final Button extendBtn = new Button("⏳ Prolonger");
            final javafx.scene.layout.HBox box = new javafx.scene.layout.HBox(4, unbanBtn, extendBtn);
            {
                unbanBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-size: 10; -fx-padding: 3 8; -fx-cursor: hand; -fx-background-radius: 4;");
                extendBtn.setStyle("-fx-background-color: #FF8C00; -fx-text-fill: white; -fx-font-size: 10; -fx-padding: 3 8; -fx-cursor: hand; -fx-background-radius: 4;");
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                } else {
                    ChatModerationService.ChatBan ban = getTableRow().getItem();
                    boolean active = ban.isActive();
                    unbanBtn.setDisable(!active);
                    extendBtn.setDisable(!active);
                    unbanBtn.setOnAction(e -> handleUnban(ban));
                    extendBtn.setOnAction(e -> handleExtendBan(ban));
                    setGraphic(box);
                }
            }
        });
    }

    @FXML
    private void refreshBannedUsers() {
        if (bannedUsersTable == null) return;
        List<ChatModerationService.ChatBan> bans = chatModerationService.getAllBans();
        bannedUsersTable.getItems().setAll(bans);
        long activeCount = bans.stream().filter(ChatModerationService.ChatBan::isActive).count();
        if (lblBanCount != null) {
            lblBanCount.setText(activeCount + " ban" + (activeCount != 1 ? "s" : "") + " actif" + (activeCount != 1 ? "s" : ""));
            lblBanCount.setStyle(activeCount > 0
                    ? "-fx-text-fill: #FF6B6B; -fx-font-size: 12; -fx-font-weight: bold;"
                    : "-fx-text-fill: #4CAF50; -fx-font-size: 12; -fx-font-weight: bold;");
        }
    }

    private void handleUnban(ChatModerationService.ChatBan ban) {
        if (!confirm("Débloquer \"" + ban.getUsername() + "\" du chat ?")) return;
        try {
            chatModerationService.unbanUser(ban.getUsername());
            showAlert("Succès", "\"" + ban.getUsername() + "\" a été débloqué.");
            refreshBannedUsers();
        } catch (java.sql.SQLException e) {
            showAlert("Erreur", "Erreur : " + e.getMessage());
        }
    }

    private void handleExtendBan(ChatModerationService.ChatBan ban) {
        org.example.utils.StyledDialog.textInput(
                "Prolonger le ban",
                "Prolonger le ban de \"" + ban.getUsername() + "\"",
                "Heures suppl\u00e9mentaires :",
                "24"
        ).ifPresent(hoursStr -> {
            try {
                int hours = Integer.parseInt(hoursStr.trim());
                if (hours <= 0) { showAlert("Erreur", "La durée doit être positive."); return; }
                chatModerationService.extendBan(ban.getUsername(), hours);
                showAlert("Succès", "Ban prolongé de " + hours + "h pour \"" + ban.getUsername() + "\".");
                refreshBannedUsers();
            } catch (NumberFormatException e) {
                showAlert("Erreur", "Veuillez entrer un nombre valide.");
            } catch (java.sql.SQLException e) {
                showAlert("Erreur", "Erreur : " + e.getMessage());
            }
        });
    }

    private void initChatAutoRefresh() {
        chatAutoRefresh = new Timeline(new KeyFrame(Duration.seconds(5), e -> {
            if (chatFeedContainer != null) refreshChatMessages();
            refreshConnectedUsers();
            refreshChatAlerts();
        }));
        chatAutoRefresh.setCycleCount(Timeline.INDEFINITE);
        chatAutoRefresh.play();
        if (txtChatSearch != null) {
            txtChatSearch.textProperty().addListener((obs, old, val) -> refreshChatMessages());
        }
    }

    @FXML
    private void refreshChatMessages() {
        if (chatFeedContainer == null) return;
        List<ChatModerationService.ChatMessage> messages = chatModerationService.getRecentMessages(100);
        String filter = (txtChatSearch != null && txtChatSearch.getText() != null)
                ? txtChatSearch.getText().trim().toLowerCase() : "";
        chatFeedContainer.getChildren().clear();
        if (messages.isEmpty()) {
                Label empty = new Label("En attente de messages...");
                empty.setStyle("-fx-text-fill: #555; -fx-font-size: 12; -fx-font-style: italic; -fx-padding: 30;");
            chatFeedContainer.getChildren().add(empty);
        } else {
            int displayed = 0;
            for (ChatModerationService.ChatMessage msg : messages) {
                if (!filter.isEmpty()
                        && !msg.getUsername().toLowerCase().contains(filter)
                        && !msg.getMessage().toLowerCase().contains(filter)) continue;
                chatFeedContainer.getChildren().add(buildChatBubble(msg));
                displayed++;
            }
            if (displayed == 0 && !filter.isEmpty()) {
                Label noMatch = new Label("🔍 Aucun résultat pour \"" + filter + "\"");
                noMatch.setStyle("-fx-text-fill: #555; -fx-font-size: 12; -fx-font-style: italic; -fx-padding: 20;");
                chatFeedContainer.getChildren().add(noMatch);
            }
        }
        if (lblMsgStats != null) lblMsgStats.setText(messages.size() + " messages");
        javafx.application.Platform.runLater(() -> {
            if (chatFeedScroll != null) chatFeedScroll.setVvalue(1.0);
        });
    }

    private javafx.scene.Node buildChatBubble(ChatModerationService.ChatMessage msg) {
        String[] palette = {"#FFD700","#4CAF50","#FFB347","#45B7D1","#DDA0DD","#FF6B6B","#4ECDC4","#FFAA00"};
        String color = palette[Math.abs(msg.getUsername().hashCode()) % palette.length];
        Label timeLabel = new Label(msg.getFormattedTime());
        timeLabel.setStyle("-fx-text-fill: #555; -fx-font-size: 10; -fx-min-width: 50;");
        Label userLabel = new Label(msg.getUsername());
        userLabel.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 11; -fx-font-weight: bold; -fx-min-width: 80; -fx-max-width: 120;");
        Label textLabel = new Label(msg.getMessage());
        textLabel.setStyle("-fx-text-fill: #ddd; -fx-font-size: 11;");
        textLabel.setWrapText(true);
        textLabel.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(textLabel, Priority.ALWAYS);
        Button delBtn = new Button("🗑");
        delBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #555; -fx-cursor: hand; -fx-font-size: 10; -fx-padding: 0 3;");
        delBtn.setOnAction(e -> deleteChatMessage(msg.getId()));
        Button kickBtn = new Button("👢");
        kickBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #555; -fx-cursor: hand; -fx-font-size: 10; -fx-padding: 0 3;");
        kickBtn.setOnAction(e -> kickChatUser(msg.getUsername()));
        Button banBtn = new Button("🚫");
        banBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #555; -fx-cursor: hand; -fx-font-size: 10; -fx-padding: 0 3;");
        banBtn.setOnAction(e -> banFromMessages(msg.getUsername()));
        HBox actions = new HBox(2, delBtn, kickBtn, banBtn);
        actions.setVisible(false);
        actions.setManaged(false);
        HBox row = new HBox(8, timeLabel, userLabel, textLabel, actions);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setStyle("-fx-padding: 4 10; -fx-background-radius: 6;");
        row.setOnMouseEntered(e -> { actions.setVisible(true); actions.setManaged(true); row.setStyle("-fx-padding: 4 10; -fx-background-radius: 6; -fx-background-color: rgba(255,215,0,0.05);"); });
        row.setOnMouseExited(e -> { actions.setVisible(false); actions.setManaged(false); row.setStyle("-fx-padding: 4 10; -fx-background-radius: 6;"); });
        return row;
    }

    private void deleteChatMessage(int messageId) {
        try {
            chatModerationService.deleteMessage(messageId);
            refreshChatMessages();
        } catch (SQLException e) {
            showAlert("Erreur", e.getMessage());
        }
    }

    private void connectAdminWebSocket() {
        try {
            URI uri = new URI("ws://localhost:" + ChatWebSocketServer.PORT);
            adminChatClient = new ChatClient(
                    uri,
                    this::onAdminWsMessage,
                    () -> {
                        adminWsConnected = true;
                        System.out.println("[Admin] Connected to chat WS");
                        javafx.application.Platform.runLater(() -> {
                            if (lblWsStatus != null) {
                                lblWsStatus.setText("● Connecté");
                                lblWsStatus.setStyle("-fx-text-fill: #4CAF50; -fx-font-size: 11; -fx-font-weight: bold;");
                            }
                        });
                        try { adminChatClient.send("{\"type\":\"get_user_list\"}"); }
                        catch (Exception e) { System.err.println("[Admin] get_user_list: " + e.getMessage()); }
                    },
                    () -> {
                        adminWsConnected = false;
                        System.out.println("[Admin] Disconnected from chat WS");
                        javafx.application.Platform.runLater(() -> {
                            if (lblWsStatus != null) {
                                lblWsStatus.setText("● Déconnecté");
                                lblWsStatus.setStyle("-fx-text-fill: #FF4444; -fx-font-size: 11; -fx-font-weight: bold;");
                            }
                        });
                    }
            );
            adminChatClient.connect();
        } catch (Exception e) {
            System.err.println("[Admin] WS connect error: " + e.getMessage());
        }
    }

    private void onAdminWsMessage(String raw) {
        javafx.application.Platform.runLater(() -> {
            if (raw == null) return;

            if (raw.contains("\"type\":\"user_list\"")) {
                parseAndDisplayUsers(raw);
            }
            if (raw.contains("\"type\":\"user_count\"")) {
                java.util.regex.Matcher m = java.util.regex.Pattern.compile("\"count\"\\s*:\\s*(\\d+)").matcher(raw);
                if (m.find()) {
                    String count = m.group(1);
                    if (lblConnectedCount != null) lblConnectedCount.setText(count);
                    if (lblOnlineStats != null) lblOnlineStats.setText(count + " en ligne");
                }
            }

            if (raw.contains("\"user\"") && raw.contains("\"text\"") && !raw.contains("\"type\":")) {

                refreshChatMessages();
            }
            if (raw.contains("\"type\":\"system\"")) {

                refreshConnectedUsers();
            }
        });
    }

    private void parseAndDisplayUsers(String raw) {
        if (adminConnectedUsersList == null) return;
        adminConnectedUsersList.getChildren().clear();
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("\"users\"\\s*:\\s*\\[(.*?)\\]").matcher(raw);
        if (m.find()) {
            String usersStr = m.group(1);
            java.util.regex.Matcher um = java.util.regex.Pattern.compile("\"([^\"]*)\"").matcher(usersStr);
            while (um.find()) {
                String username = um.group(1);
                Label userLabel = new Label("● " + username);
                userLabel.setStyle("-fx-text-fill: #FFD700; -fx-font-size: 11; -fx-font-weight: bold;");
                Button kickBtn = new Button("👢");
                kickBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #FF8C00; -fx-cursor: hand; -fx-font-size: 10; -fx-padding: 0 4;");
                kickBtn.setOnAction(e -> kickChatUser(username));
                Button banBtn = new Button("🚫");
                banBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #FF4444; -fx-cursor: hand; -fx-font-size: 10; -fx-padding: 0 4;");
                banBtn.setOnAction(e -> banFromMessages(username));
                Region spacer = new Region();
                HBox.setHgrow(spacer, Priority.ALWAYS);
                HBox row = new HBox(6, userLabel, spacer, kickBtn, banBtn);
                row.setAlignment(Pos.CENTER_LEFT);
                row.setStyle("-fx-padding: 3 8; -fx-background-radius: 6;");
                row.setOnMouseEntered(e -> row.setStyle("-fx-padding: 3 8; -fx-background-radius: 6; -fx-background-color: rgba(255,215,0,0.06);"));
                row.setOnMouseExited(e -> row.setStyle("-fx-padding: 3 8; -fx-background-radius: 6;"));
                adminConnectedUsersList.getChildren().add(row);
            }
        }
        if (adminConnectedUsersList.getChildren().isEmpty()) {
            Label empty = new Label("Aucun utilisateur en ligne");
            empty.setStyle("-fx-text-fill: #555; -fx-font-size: 11; -fx-font-style: italic; -fx-padding: 8;");
            adminConnectedUsersList.getChildren().add(empty);
        }
    }

    private void refreshConnectedUsers() {

        if (adminChatClient != null && adminChatClient.isOpen()) {
            adminChatClient.send("{\"type\":\"get_user_list\"}");
        }
    }

    private void kickChatUser(String username) {
        if (!confirm("Expulser \"" + username + "\" du chat ?")) return;
        if (adminChatClient == null || !adminChatClient.isOpen()) {
            showAlert("Erreur", "Pas connecté au serveur de chat.");
            return;
        }
        String payload = "{\"type\":\"admin_kick\",\"target\":\"" + escapeJson(username) + "\"}";
        adminChatClient.send(payload);
        showAlert("Succès", "\"" + username + "\" a été expulsé du chat.");
    }

    private void banFromMessages(String username) {
        org.example.utils.StyledDialog.textInput(
                "Bannir l'utilisateur",
                "Bannir \"" + username + "\" du chat",
                "Dur\u00e9e du ban (heures) :",
                "24"
        ).ifPresent(hoursStr -> {
            try {
                int hours = Integer.parseInt(hoursStr.trim());
                if (hours <= 0) { showAlert("Erreur", "La durée doit être positive."); return; }
                chatModerationService.banUser(username, hours, "Banni par l'admin depuis le panneau de contrôle");

                if (adminChatClient != null && adminChatClient.isOpen()) {
                    adminChatClient.send("{\"type\":\"admin_kick\",\"target\":\"" + escapeJson(username) + "\"}");
                }
                showAlert("Succès", "\"" + username + "\" a été banni du chat pour " + hours + "h.");
                refreshChatAlerts();
                refreshChatMessages();
            } catch (NumberFormatException e) {
                showAlert("Erreur", "Veuillez entrer un nombre valide.");
            } catch (SQLException e) {
                showAlert("Erreur", "Erreur : " + e.getMessage());
            }
        });
    }

    @FXML
    private void handleSendAdminMessage() {
        if (adminMessageField == null) return;
        String text = adminMessageField.getText() != null ? adminMessageField.getText().trim() : "";
        if (text.isEmpty()) {
            showAlert("Attention", "Saisissez un message.");
            return;
        }
        if (adminChatClient == null || !adminChatClient.isOpen()) {
            showAlert("Erreur", "Pas connecté au serveur de chat.");
            return;
        }
        String payload = "{\"type\":\"admin_message\",\"text\":\"" + escapeJson(text) + "\"}";
        adminChatClient.send(payload);
        adminMessageField.clear();
        showAlert("Succès", "Annonce envoyée à tous les utilisateurs du chat.");
    }

    @FXML
    private void handleAdminClearChat() {
        if (!confirm("Vider le chat pour tous les utilisateurs ? Cette action est irréversible.")) return;
        if (adminChatClient == null || !adminChatClient.isOpen()) {
            showAlert("Erreur", "Pas connecté au serveur de chat.");
            return;
        }
        adminChatClient.send("{\"type\":\"admin_clear\"}");
        try {
            chatModerationService.clearAllMessages();
        } catch (SQLException e) {
            System.err.println("[Admin] Clear DB messages: " + e.getMessage());
        }
        refreshChatMessages();
        showAlert("Succès", "Le chat a été vidé.");
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }

    private boolean confirm(String message) {
        return org.example.utils.StyledDialog.confirm(message);
    }

    private void showAlert(String title, String message) {
        org.example.utils.StyledDialog.show(title, message);
    }
}
