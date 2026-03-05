package com.esprit.chat;

import org.java_websocket.server.WebSocketServer;
import org.java_websocket.handshake.ClientHandshake;
import com.esprit.services.ForumChatModerationService;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.Deque;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ChatWebSocketServer extends WebSocketServer {

    public static final int PORT = 8887;
    public static final int HISTORY_SIZE = 10;

    private final Map<org.java_websocket.WebSocket, String> connectionToUser = new ConcurrentHashMap<>();

    private final Deque<String> messageHistory = new LinkedList<>();

    private ForumChatModerationService chatModerationService;

    public ChatWebSocketServer() {
        super(new InetSocketAddress(PORT));
        setReuseAddr(true);
        try {
            this.chatModerationService = new ForumChatModerationService();
        } catch (Exception e) {
            System.err.println("[Chat] ChatModerationService init error (DB may be unavailable): " + e.getMessage());
            this.chatModerationService = null;
        }
    }

    @Override
    public void onOpen(org.java_websocket.WebSocket conn, ClientHandshake handshake) {
        System.out.println("[Chat] Nouveau client connecté: " + conn.getRemoteSocketAddress() + " | Total connections: " + getConnections().size());
    }

    @Override
    public void onClose(org.java_websocket.WebSocket conn, int code, String reason, boolean remote) {
        String user = connectionToUser.remove(conn);
        if (user != null) {
            int n = connectionToUser.size();
            broadcast("{\"type\":\"system\",\"message\":\"" + escapeJson(user) + " a quitté le salon.\",\"count\":" + n + "}");
            broadcastUserCount();
            broadcastUserList();

            if (n == 0) {
                synchronized (messageHistory) {
                    messageHistory.clear();
                }
                System.out.println("[Chat] Tous les utilisateurs ont quitté — historique effacé.");
            }
        }
        System.out.println("[Chat] Client déconnecté: " + conn.getRemoteSocketAddress());
    }

    @Override
    public void onMessage(org.java_websocket.WebSocket conn, String message) {
        if (message == null || message.isBlank()) return;
        if (message.contains("\"type\":\"join\"") || message.contains("\"type\": \"join\"")) {
            String user = extractJoinUser(message);
            if (user != null && !user.isBlank()) {
                connectionToUser.put(conn, user);
                sendHistoryTo(conn);
                int n = connectionToUser.size();
                System.out.println("[Chat] User '" + user + "' joined. connectionToUser.size()=" + n + " getConnections().size()=" + getConnections().size());
                String systemMsg = "{\"type\":\"system\",\"message\":\"" + escapeJson(user) + " a rejoint le salon.\",\"count\":" + n + "}";
                System.out.println("[Chat] Broadcasting system msg: " + systemMsg);
                broadcast(systemMsg);
                broadcastUserCount();
                broadcastUserList();
                try {
                    String directCount = "{\"type\":\"user_count\",\"count\":" + connectionToUser.size() + "}";
                    conn.send(directCount);
                } catch (Exception e) { System.err.println("[Chat] direct count error: " + e.getMessage()); }
            }
            return;
        }
        if (message.contains("\"type\":\"get_count\"") || message.contains("\"type\": \"get_count\"")) {
            try {
                conn.send("{\"type\":\"user_count\",\"count\":" + connectionToUser.size() + "}");
            } catch (Exception e) { System.err.println("[Chat] get_count: " + e.getMessage()); }
            return;
        }
        if (message.contains("\"type\":\"get_user_list\"")) {
            try {
                StringBuilder sb = new StringBuilder("{\"type\":\"user_list\",\"users\":[");
                boolean first = true;
                for (String u : connectionToUser.values()) {
                    if (!first) sb.append(",");
                    sb.append("\"").append(escapeJson(u)).append("\"");
                    first = false;
                }
                sb.append("]}");
                conn.send(sb.toString());
                conn.send("{\"type\":\"user_count\",\"count\":" + connectionToUser.size() + "}");
            } catch (Exception e) { System.err.println("[Chat] get_user_list: " + e.getMessage()); }
            return;
        }

        if (message.contains("\"type\":\"admin_kick\"")) {
            handleAdminKick(message);
            return;
        }
        if (message.contains("\"type\":\"admin_message\"")) {
            handleAdminMessage(message);
            return;
        }
        if (message.contains("\"type\":\"admin_clear\"")) {
            handleAdminClear();
            return;
        }

        if (isNormalMessage(message)) {
            synchronized (messageHistory) {
                messageHistory.addLast(message);
                while (messageHistory.size() > HISTORY_SIZE) messageHistory.pollFirst();
            }

            String user = extractUser(message);
            String text = extractText(message);
            if (chatModerationService != null && user != null && text != null) {
                try { chatModerationService.saveMessage(user, text); } catch (Exception e) {
                    System.err.println("[Chat] Save message to DB: " + e.getMessage());
                }
            }
        }
        broadcast(message);
    }

    private void handleAdminKick(String message) {
        String target = extractField(message, "target");
        if (target == null || target.isBlank()) return;
        System.out.println("[Chat] Admin KICK: " + target);
        org.java_websocket.WebSocket targetConn = null;
        for (Map.Entry<org.java_websocket.WebSocket, String> entry : connectionToUser.entrySet()) {
            if (target.equals(entry.getValue())) {
                targetConn = entry.getKey();
                break;
            }
        }
        if (targetConn != null) {
            try {
                targetConn.send("{\"type\":\"kicked\",\"message\":\"Vous avez été expulsé du chat par l'administrateur.\"}");
            } catch (Exception ignored) {}
            connectionToUser.remove(targetConn);
            try { targetConn.close(); } catch (Exception ignored) {}
            int n = connectionToUser.size();
            broadcast("{\"type\":\"system\",\"message\":\"" + escapeJson(target) + " a été expulsé par l'administrateur.\",\"count\":" + n + "}");
            broadcastUserCount();
            broadcastUserList();
        }
    }

    private void handleAdminMessage(String message) {
        String text = extractField(message, "text");
        if (text == null || text.isBlank()) return;
        System.out.println("[Chat] Admin MESSAGE: " + text);
        String announcement = "{\"type\":\"admin_announcement\",\"message\":\"" + escapeJson(text) + "\"}";
        broadcast(announcement);
    }

    private void handleAdminClear() {
        System.out.println("[Chat] Admin CLEAR");
        synchronized (messageHistory) {
            messageHistory.clear();
        }
        broadcast("{\"type\":\"admin_clear\"}");
    }

    private static boolean isNormalMessage(String msg) {
        return msg.contains("\"user\"") && msg.contains("\"text\"") && !msg.contains("\"type\":");
    }

    private void sendHistoryTo(org.java_websocket.WebSocket conn) {
        if (conn == null || !conn.isOpen()) return;
        String[] snapshot;
        synchronized (messageHistory) {
            snapshot = messageHistory.toArray(new String[0]);
        }
        int count = snapshot.length;
        try {
            conn.send("{\"type\":\"history_start\",\"count\":" + count + "}");
            for (String msg : snapshot) {
                conn.send(msg);
            }
            conn.send("{\"type\":\"history_end\"}");
        } catch (Exception e) {
            System.err.println("[Chat] Envoi historique: " + e.getMessage());
        }
    }

    private void broadcastUserCount() {
        int n = connectionToUser.size();
        String msg = "{\"type\":\"user_count\",\"count\":" + n + "}";
        for (org.java_websocket.WebSocket c : getConnections()) {
            try {
                if (c != null && c.isOpen()) c.send(msg);
            } catch (Exception e) {
                System.err.println("[Chat] user_count send error: " + e.getMessage());
            }
        }
    }

    private void broadcastUserList() {
        StringBuilder sb = new StringBuilder("{\"type\":\"user_list\",\"users\":[");
        boolean first = true;
        for (String u : connectionToUser.values()) {
            if (!first) sb.append(",");
            sb.append("\"").append(escapeJson(u)).append("\"");
            first = false;
        }
        sb.append("]}");
        String msg = sb.toString();
        for (org.java_websocket.WebSocket c : getConnections()) {
            try {
                if (c != null && c.isOpen()) c.send(msg);
            } catch (Exception e) {
                System.err.println("[Chat] user_list send error: " + e.getMessage());
            }
        }
    }

    private static String extractJoinUser(String json) {
        java.util.regex.Pattern p = java.util.regex.Pattern.compile("\"user\"\\s*:\\s*\"([^\"]*)\"");
        java.util.regex.Matcher m = p.matcher(json);
        return m.find() ? m.group(1).replace("\\\"", "\"") : null;
    }

    private static String extractUser(String json) {
        return extractField(json, "user");
    }

    private static String extractText(String json) {
        return extractField(json, "text");
    }

    private static String extractField(String json, String field) {
        if (json == null) return null;
        java.util.regex.Pattern p = java.util.regex.Pattern.compile("\"" + field + "\"\\s*:\\s*\"([^\"]*)\"");
        java.util.regex.Matcher m = p.matcher(json);
        return m.find() ? m.group(1).replace("\\\"", "\"").replace("\\\\", "\\") : null;
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }

    public Map<org.java_websocket.WebSocket, String> getConnectionToUser() {
        return connectionToUser;
    }

    @Override
    public void onMessage(org.java_websocket.WebSocket conn, ByteBuffer message) {
    }

    @Override
    public void onError(org.java_websocket.WebSocket conn, Exception ex) {
        if (ex != null) System.err.println("[Chat Server] Erreur: " + ex.getMessage());
    }

    @Override
    public void onStart() {
        System.out.println("[Chat] Serveur WebSocket démarré sur le port " + PORT);
    }
}
