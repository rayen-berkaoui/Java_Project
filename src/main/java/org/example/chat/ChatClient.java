package org.example.chat;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.util.function.Consumer;

public class ChatClient extends WebSocketClient {

    private final Consumer<String> onMessageReceived;
    private final Runnable onConnectionClosed;
    private final Runnable onConnectionOpened;

    public ChatClient(URI serverUri, Consumer<String> onMessageReceived,
                      Runnable onConnectionOpened, Runnable onConnectionClosed) {
        super(serverUri);
        this.onMessageReceived = onMessageReceived;
        this.onConnectionOpened = onConnectionOpened;
        this.onConnectionClosed = onConnectionClosed;
    }

    @Override
    public void onOpen(ServerHandshake handshake) {
        if (onConnectionOpened != null) onConnectionOpened.run();
    }

    @Override
    public void onMessage(String message) {
        if (onMessageReceived != null && message != null) {
            onMessageReceived.accept(message);
        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        if (onConnectionClosed != null) onConnectionClosed.run();
    }

    @Override
    public void onError(Exception ex) {
        if (ex != null) System.err.println("[Chat Client] " + ex.getMessage());
    }

    public void sendJoin(String user) {
        String payload = String.format("{\"type\":\"join\",\"user\":\"%s\"}", escapeJson(user));
        send(payload);
    }

    public void sendGetCount() {
        send("{\"type\":\"get_count\"}");
    }

    public void sendMessage(String user, String text) {
        long time = System.currentTimeMillis();
        String payload = String.format("{\"user\":\"%s\",\"text\":\"%s\",\"time\":%d}",
                escapeJson(user), escapeJson(text), time);
        send(payload);
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }
}
