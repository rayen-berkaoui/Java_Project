package Controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import org.example.chat.ChatClient;
import org.example.chat.ChatWebSocketServer;
import org.example.services.ChatModerationService;

import java.net.URI;
import java.time.LocalDateTime;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChatController implements Initializable {

    @FXML private VBox messagesContainer;
    @FXML private ScrollPane scrollPane;
    @FXML private TextField messageInput;
    @FXML private TextField usernameField;
    @FXML private Label statusLabel;
    @FXML private Button joinButton;
    @FXML private Button sendButton;
    @FXML private Label userCountLabel;
    @FXML private VBox usersListContainer;

    private AppNavigator navigator;
    private ChatClient chatClient;
    private boolean joined = false;
    private final ChatModerationService chatModeration = new ChatModerationService();

    private int expectHistory = -1;

    private volatile boolean countSyncRunning = false;

    private static final String WS_URL = "ws://localhost:" + ChatWebSocketServer.PORT;
    private static final Pattern JSON_MSG = Pattern.compile("\"user\"\\s*:\\s*\"([^\"]*)\"\\s*,\\s*\"text\"\\s*:\\s*\"([^\"]*)\"");
    private static final Pattern JSON_TIME = Pattern.compile("\"time\"\\s*:\\s*(\\d+)");
    private static final Pattern JSON_SYSTEM = Pattern.compile("\"type\"\\s*:\\s*\"system\"\\s*,\\s*\"message\"\\s*:\\s*\"([^\"]*)\"");
    private static final Pattern JSON_HISTORY_START = Pattern.compile("\"type\"\\s*:\\s*\"history_start\"\\s*,\\s*\"count\"\\s*:\\s*(\\d+)");
    private static final Pattern JSON_USER_COUNT = Pattern.compile("\"type\"\\s*:\\s*\"user_count\"\\s*,\\s*\"count\"\\s*:\\s*(\\d+)");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault());

    private static final String[] USER_COLORS = {
        "#FFB347", "#98D8C8", "#F7DC6F", "#BB8FCE", "#85C1E9",
        "#F8B500", "#00CED1", "#FF6B6B", "#4ECDC4", "#95E1D3"
    };

    @Override
    public void initialize(java.net.URL url, java.util.ResourceBundle resourceBundle) {
        setJoinedState(false);
        connect();
    }

    public void setNavigator(AppNavigator navigator) {
        this.navigator = navigator;
    }

    private void connect() {
        try {
            URI uri = new URI(WS_URL);
            chatClient = new ChatClient(
                    uri,
                    this::onMessageReceived,
                    this::onConnectionOpened,
                    this::onConnectionClosed
            );
            chatClient.connect();
        } catch (Exception e) {
            Platform.runLater(() -> setStatus("Erreur: " + e.getMessage()));
        }
    }

    private void onConnectionOpened() {
        Platform.runLater(() -> {
            if (messagesContainer != null) messagesContainer.getChildren().clear();
            expectHistory = -1;
            setStatus("Connecté — entrez votre pseudo et rejoignez");
        });
    }

    private void setJoinedState(boolean isJoined) {
        joined = isJoined;
        if (messageInput != null) messageInput.setDisable(!isJoined);
        if (sendButton != null) sendButton.setDisable(!isJoined);
        if (usernameField != null) usernameField.setDisable(isJoined);
        if (joinButton != null) {
            joinButton.setDisable(isJoined);
            joinButton.setVisible(!isJoined);
            joinButton.setManaged(!isJoined);
        }
        if (messageInput != null) messageInput.setPromptText(isJoined ? "Écrivez votre message..." : "Rejoignez le salon pour écrire...");
    }

    @FXML
    private void joinSalon() {
        if (chatClient == null || !chatClient.isOpen()) {
            showAlert("Erreur", "Pas encore connecté au serveur.");
            return;
        }
        String user = usernameField != null ? usernameField.getText() : "";
        if (user == null) user = "";
        user = user.trim();
        if (user.isEmpty()) {
            showAlert("Pseudo obligatoire", "Veuillez entrer un pseudo pour rejoindre le salon.");
            return;
        }

        LocalDateTime bannedUntil = chatModeration.checkBanned(user);
        if (bannedUntil != null) {
            String until = bannedUntil.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
            showAlert("Accès refusé", "Vous êtes banni du chat jusqu'au " + until
                    + " en raison de langage inapproprié.");
            return;
        }
        chatClient.sendJoin(user);
        setJoinedState(true);
        setStatus("Connecté en tant que \"" + user + "\"");
        startCountSync();
    }

    private void showAlert(String title, String message) {
        org.example.utils.StyledDialog.show(title, message);
    }

    private void onConnectionClosed() {
        Platform.runLater(() -> setStatus("Déconnecté"));
    }

    private void onMessageReceived(String raw) {
        Platform.runLater(() -> handleIncomingMessage(raw));
    }

    private void setStatus(String text) {
        if (statusLabel != null) statusLabel.setText(text);
    }

    private void updateUserCount(int count) {
        if (userCountLabel != null) {
            userCountLabel.setText(count + " connecté(s)");
        }
    }

    private void startCountSync() {
        countSyncRunning = true;
        Thread syncThread = new Thread(() -> {

            try {
                if (chatClient != null && chatClient.isOpen()) chatClient.sendGetCount();
                Thread.sleep(1000);
                if (chatClient != null && chatClient.isOpen()) chatClient.sendGetCount();
            } catch (InterruptedException e) { return; }

            while (countSyncRunning) {
                try {
                    Thread.sleep(10000);
                    if (chatClient != null && chatClient.isOpen()) {
                        chatClient.sendGetCount();
                    } else {
                        break;
                    }
                } catch (InterruptedException e) { return; }
            }
        });
        syncThread.setDaemon(true);
        syncThread.setName("ChatCountSync");
        syncThread.start();
    }

    private void stopCountSync() {
        countSyncRunning = false;
    }

    private void handleIncomingMessage(String raw) {
        if (messagesContainer == null) return;
        if (raw == null) return;

        if (raw.contains("\"count\"")) {
            int anyCount = extractCount(raw);
            if (anyCount >= 0) {
                updateUserCount(anyCount);
            }
        }

        if (raw.contains("get_count")) return;
        if (raw.contains("\"type\":\"join\"") || raw.contains("\"type\": \"join\"")) {
            return;
        }
        if (raw.contains("user_count")) {

            return;
        }
        if (raw.contains("user_list")) {
            updateUserList(raw);
            return;
        }
        if (raw.contains("\"type\":\"history_start\"") || raw.contains("\"type\": \"history_start\"")) {
            Matcher m = JSON_HISTORY_START.matcher(raw);
            if (m.find()) expectHistory = Integer.parseInt(m.group(1));
            addBubble("", "— Historique (" + expectHistory + " message(s)) —", true, null);
            return;
        }
        if (raw.contains("\"type\":\"history_end\"") || raw.contains("\"type\": \"history_end\"")) {
            expectHistory = -1;
            scrollToBottom();
            return;
        }

        if (raw.contains("\"type\":\"kicked\"")) {
            Matcher km = Pattern.compile("\"message\"\\s*:\\s*\"([^\"]*)\"").matcher(raw);
            String kickMsg = km.find() ? unescapeJson(km.group(1)) : "Vous avez été expulsé du chat.";
            setJoinedState(false);
            stopCountSync();
            if (chatClient != null) { chatClient.close(); chatClient = null; }
            showAlert("Expulsé", kickMsg);
            if (navigator != null) navigator.showTabaaniConnect();
            return;
        }

        if (raw.contains("\"type\":\"admin_clear\"")) {
            if (messagesContainer != null) messagesContainer.getChildren().clear();
            addBubble("", "💬 Le chat a été vidé par l'administrateur.", true, null);
            return;
        }

        if (raw.contains("\"type\":\"admin_announcement\"")) {
            Matcher am = Pattern.compile("\"message\"\\s*:\\s*\"([^\"]*)\"").matcher(raw);
            if (am.find()) {
                String announcement = unescapeJson(am.group(1));
                addAdminAnnouncement(announcement);
                scrollToBottom();
            }
            return;
        }

        Matcher msgMatcher = JSON_MSG.matcher(raw);
        Matcher sysMatcher = JSON_SYSTEM.matcher(raw);

        if (msgMatcher.find()) {
            String user = unescapeJson(msgMatcher.group(1));
            String text = unescapeJson(msgMatcher.group(2));
            String timeStr = null;
            Matcher tm = JSON_TIME.matcher(raw);
            if (tm.find()) {
                try {
                    long millis = Long.parseLong(tm.group(1));
                    timeStr = TIME_FMT.format(Instant.ofEpochMilli(millis));
                } catch (NumberFormatException ignored) { }
            }
            addBubble(user, text, false, timeStr);
        } else if (sysMatcher.find()) {
            String message = unescapeJson(sysMatcher.group(1));
            addBubble("", message, true, null);

        } else {
            addBubble("", raw, true, null);
        }

        if (expectHistory >= 0) {
            expectHistory--;
        } else {
            scrollToBottom();
        }
    }

    private void addBubble(String user, String text, boolean system, String timeStr) {
        if (system) {
            Label line = new Label();
            line.setWrapText(true);
            line.maxWidthProperty().bind(messagesContainer.widthProperty().subtract(32));
            line.setStyle("-fx-text-fill: #888; -fx-font-size: 11; -fx-font-style: italic;");
            line.setText("  " + text);
            messagesContainer.getChildren().add(line);
        } else {
            String color = getColorForUser(user);
            TextFlow flow = new TextFlow();
            flow.maxWidthProperty().bind(messagesContainer.widthProperty().subtract(32));
            flow.setStyle("-fx-font-size: 13;");
            String prefix = (timeStr != null ? "[" + timeStr + "] " : "") + user + " : ";
            Text t1 = new Text(prefix);
            t1.setFill(Color.web(color));
            t1.setStyle("-fx-font-weight: bold;");
            Text t2 = new Text(text);
            t2.setFill(Color.web("#e0e0e0"));
            flow.getChildren().addAll(t1, t2);
            messagesContainer.getChildren().add(flow);
        }
    }

    private void addAdminAnnouncement(String text) {
        Label line = new Label();
        line.setWrapText(true);
        line.maxWidthProperty().bind(messagesContainer.widthProperty().subtract(32));
        line.setStyle("-fx-text-fill: #FFD700; -fx-font-size: 13; -fx-font-weight: bold; "
                + "-fx-background-color: rgba(255,215,0,0.1); -fx-padding: 6 12; -fx-background-radius: 6;");
        line.setText("📢 ADMIN : " + text);
        messagesContainer.getChildren().add(line);
    }

    private static String getColorForUser(String user) {
        if (user == null || user.isEmpty()) return "#e0e0e0";
        int i = Math.abs(user.hashCode()) % USER_COLORS.length;
        return USER_COLORS[i];
    }

    private void scrollToBottom() {
        if (scrollPane != null) {
            scrollPane.setVvalue(1.0);
        }
    }

    private void updateUserList(String raw) {
        if (usersListContainer == null) return;
        usersListContainer.getChildren().clear();

        java.util.regex.Matcher m = java.util.regex.Pattern.compile("\"users\"\\s*:\\s*\\[(.*?)\\]").matcher(raw);
        if (m.find()) {
            String usersStr = m.group(1);
            java.util.regex.Matcher um = java.util.regex.Pattern.compile("\"([^\"]*)\"").matcher(usersStr);
            while (um.find()) {
                String username = unescapeJson(um.group(1));
                String color = getColorForUser(username);
                Label userLabel = new Label("  \u25CF " + username);
                userLabel.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 13; -fx-font-weight: bold;");
                userLabel.setWrapText(true);
                usersListContainer.getChildren().add(userLabel);
            }
        }
    }

    private static int extractCount(String raw) {
        if (raw == null) return -1;

        Matcher m = Pattern.compile("\"count\"\\s*:\\s*(\\d+)").matcher(raw);
        if (m.find()) {
            try { return Integer.parseInt(m.group(1)); } catch (NumberFormatException e) {  }
        }

        int idx = raw.indexOf("\"count\"");
        if (idx >= 0) {
            int colon = raw.indexOf(':', idx + 7);
            if (colon >= 0) {
                StringBuilder sb = new StringBuilder();
                for (int i = colon + 1; i < raw.length(); i++) {
                    char c = raw.charAt(i);
                    if (Character.isDigit(c)) sb.append(c);
                    else if (sb.length() > 0) break;
                }
                if (sb.length() > 0) {
                    try { return Integer.parseInt(sb.toString()); } catch (NumberFormatException e) {  }
                }
            }
        }
        return -1;
    }

    private static String unescapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\n", "\n").replace("\\r", "\r").replace("\\\"", "\"").replace("\\\\", "\\");
    }

    @FXML
    private void sendMessage() {
        if (!joined || chatClient == null || !chatClient.isOpen()) return;
        String text = messageInput != null ? messageInput.getText() : "";
        if (text == null) text = "";
        text = text.trim();
        if (text.isEmpty()) return;

        String user = usernameField != null ? usernameField.getText() : "";
        if (user == null || user.isBlank()) user = "Anonyme";

        LocalDateTime bannedUntil = chatModeration.checkBanned(user);
        if (bannedUntil != null) {
            String until = bannedUntil.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
            showAlert("Banni", "Vous êtes banni du chat jusqu'au " + until + ".");
            messageInput.clear();
            return;
        }

        String badWord = chatModeration.checkMessage(user, text);
        if (badWord != null) {
            showAlert("Message bloqué",
                    "Votre message contient du langage inapproprié (\"" + badWord + "\").\n"
                    + "L'administrateur a été notifié.\nAttention : les récidives entraîneront un bannissement.");
            messageInput.clear();
            return;
        }

        chatClient.sendMessage(user, text);
        messageInput.clear();
    }

    @FXML
    private void goBack() {
        stopCountSync();
        if (chatClient != null) {
            chatClient.close();
            chatClient = null;
        }
        if (navigator != null) navigator.showTabaaniConnect();
    }
}
