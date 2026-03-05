package com.esprit.services;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.vosk.LibVosk;
import org.vosk.LogLevel;
import org.vosk.Model;
import org.vosk.Recognizer;

import javax.sound.sampled.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Offline AI speech recognition service using Vosk.
 * Supports French language. Downloads the model automatically on first use.
 * No API key required — entirely local neural network inference.
 */
public class SpeechRecognitionService {

    private static final String MODEL_DIR_NAME = "vosk-model-small-fr-0.22";
    private static final String MODEL_ZIP_URL =
            "https://alphacephei.com/vosk/models/vosk-model-small-fr-0.22.zip";
    private static final float SAMPLE_RATE = 16000;

    private Model model;
    private volatile boolean recording = false;
    private Thread captureThread;
    private Thread recognitionThread;
    private final LinkedBlockingQueue<byte[]> audioQueue = new LinkedBlockingQueue<>(200);

    /** Callback interface for transcription events. */
    public interface TranscriptionListener {
        void onPartialResult(String partialText);
        void onFinalResult(String finalText);
        void onError(String errorMessage);
    }

    public SpeechRecognitionService() {
        // Suppress Vosk verbose logging
        try {
            LibVosk.setLogLevel(LogLevel.WARNINGS);
        } catch (Exception ignored) {}
    }

    // ========== Model Management ==========

    /** Path where the model is stored: ~/.vosk-models/vosk-model-small-fr-0.22 */
    private Path getModelDirectory() {
        return Paths.get(System.getProperty("user.home"), ".vosk-models", MODEL_DIR_NAME);
    }

    /** Check if the French speech model is already downloaded. */
    public boolean isModelDownloaded() {
        Path modelDir = getModelDirectory();
        return Files.exists(modelDir) && Files.isDirectory(modelDir);
    }

    /**
     * Download and extract the French speech model (~41 MB).
     * @param progressCallback receives values 0.0–1.0 for download progress, -1.0 for extracting
     */
    public void downloadModel(Consumer<Double> progressCallback) throws IOException {
        Path modelsRoot = getModelDirectory().getParent();
        Files.createDirectories(modelsRoot);
        Path zipFile = modelsRoot.resolve(MODEL_DIR_NAME + ".zip");

        // Download zip
        URL url = new URL(MODEL_ZIP_URL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestProperty("User-Agent", "JavaFX-Tourism-App/1.0");
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(30000);
        long totalSize = conn.getContentLengthLong();
        long downloaded = 0;

        try (InputStream in = conn.getInputStream();
             FileOutputStream out = new FileOutputStream(zipFile.toFile())) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
                downloaded += bytesRead;
                if (totalSize > 0 && progressCallback != null) {
                    progressCallback.accept((double) downloaded / totalSize);
                }
            }
        }

        // Extract zip
        if (progressCallback != null) progressCallback.accept(-1.0);
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile.toFile()))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                Path entryPath = modelsRoot.resolve(entry.getName());
                // Security: prevent zip-slip
                if (!entryPath.normalize().startsWith(modelsRoot.normalize())) continue;
                if (entry.isDirectory()) {
                    Files.createDirectories(entryPath);
                } else {
                    Files.createDirectories(entryPath.getParent());
                    Files.copy(zis, entryPath, StandardCopyOption.REPLACE_EXISTING);
                }
                zis.closeEntry();
            }
        }

        // Clean up zip
        Files.deleteIfExists(zipFile);
        System.out.println("✅ Vosk French model downloaded and extracted successfully.");
    }

    /**
     * Initialize the Vosk model. Must be called before startRecording().
     * @return true if model loaded successfully
     */
    public boolean initializeModel() {
        try {
            if (model != null) return true;
            Path modelDir = getModelDirectory();
            if (!Files.exists(modelDir)) return false;
            model = new Model(modelDir.toString());
            System.out.println("✅ Vosk model initialized.");
            return true;
        } catch (Exception e) {
            System.err.println("❌ Failed to initialize Vosk model: " + e.getMessage());
            return false;
        }
    }

    // ========== Recording & Transcription ==========

    /**
     * Start recording from the microphone and transcribing in real time.
     * Runs in a background daemon thread.
     * @param listener receives partial/final transcription results
     */
    public void startRecording(TranscriptionListener listener) {
        if (recording) return;
        if (model == null) {
            if (listener != null) listener.onError("Le modèle vocal n'est pas initialisé");
            return;
        }

        recording = true;
        audioQueue.clear();

        // Thread 1: Capture audio from microphone (high priority, never blocked by recognition)
        captureThread = new Thread(() -> {
            AudioFormat format = new AudioFormat(SAMPLE_RATE, 16, 1, true, false);
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);

            if (!AudioSystem.isLineSupported(info)) {
                if (listener != null) listener.onError("Microphone non disponible");
                recording = false;
                return;
            }

            TargetDataLine microphone = null;
            try {
                microphone = (TargetDataLine) AudioSystem.getLine(info);
                // Large internal buffer (0.5s) to prevent OS-level audio drops
                int lineBufferSize = (int) (SAMPLE_RATE * 2 * 0.5);
                microphone.open(format, lineBufferSize);
                microphone.start();

                byte[] buffer = new byte[8192]; // ~256ms chunks
                while (recording) {
                    int bytesRead = microphone.read(buffer, 0, buffer.length);
                    if (bytesRead > 0) {
                        byte[] chunk = new byte[bytesRead];
                        System.arraycopy(buffer, 0, chunk, 0, bytesRead);
                        // Non-blocking offer; if queue is full, drop oldest to stay real-time
                        if (!audioQueue.offer(chunk)) {
                            audioQueue.poll();
                            audioQueue.offer(chunk);
                        }
                    }
                }
                microphone.stop();
            } catch (Exception e) {
                if (listener != null) listener.onError("Erreur micro: " + e.getMessage());
            } finally {
                if (microphone != null) {
                    try { microphone.close(); } catch (Exception ignored) {}
                }
            }
        });
        captureThread.setDaemon(true);
        captureThread.setName("vosk-audio-capture");
        captureThread.setPriority(Thread.MAX_PRIORITY);
        captureThread.start();

        // Thread 2: Recognition (processes audio chunks from the queue)
        recognitionThread = new Thread(() -> {
            Recognizer recognizer = null;
            try {
                recognizer = new Recognizer(model, SAMPLE_RATE);
                StringBuilder fullText = new StringBuilder();

                while (recording || !audioQueue.isEmpty()) {
                    byte[] chunk = audioQueue.poll(100, java.util.concurrent.TimeUnit.MILLISECONDS);
                    if (chunk == null) continue;

                    if (recognizer.acceptWaveForm(chunk, chunk.length)) {
                        String result = extractText(recognizer.getResult(), "text");
                        if (!result.isEmpty()) {
                            if (fullText.length() > 0) fullText.append(" ");
                            fullText.append(result);
                            if (listener != null) listener.onPartialResult(fullText.toString());
                        }
                    } else {
                        String partial = extractText(recognizer.getPartialResult(), "partial");
                        if (!partial.isEmpty() && listener != null) {
                            String combined = fullText.length() > 0
                                    ? fullText + " " + partial : partial;
                            listener.onPartialResult(combined);
                        }
                    }
                }

                // Finalize remaining audio
                String finalPart = extractText(recognizer.getFinalResult(), "text");
                if (!finalPart.isEmpty()) {
                    if (fullText.length() > 0) fullText.append(" ");
                    fullText.append(finalPart);
                }

                if (listener != null) {
                    listener.onFinalResult(fullText.toString().trim());
                }

            } catch (Exception e) {
                if (listener != null) listener.onError("Erreur reconnaissance: " + e.getMessage());
            } finally {
                if (recognizer != null) {
                    try { recognizer.close(); } catch (Exception ignored) {}
                }
            }
        });
        recognitionThread.setDaemon(true);
        recognitionThread.setName("vosk-recognition");
        recognitionThread.start();
    }

    /** Stop the current recording. */
    public void stopRecording() {
        recording = false;
    }

    /** @return true if currently recording */
    public boolean isRecording() {
        return recording;
    }

    /**
     * Extract text from Vosk JSON result.
     * Vosk returns: {"text": "bonjour"} or {"partial": "bon"}
     * Fixes encoding: Vosk native lib returns UTF-8 bytes that JNI may
     * interpret as ISO-8859-1, causing accented chars to be garbled.
     */
    private String extractText(String json, String key) {
        try {
            JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
            if (obj.has(key)) {
                String raw = obj.get(key).getAsString().trim();
                return fixEncoding(raw);
            }
            return "";
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Fix UTF-8 text that was incorrectly decoded as ISO-8859-1.
     * e.g. "protÃ©gÃ©e" → "protégée"
     */
    private String fixEncoding(String text) {
        if (text == null || text.isEmpty()) return text;
        try {
            // Check if the text contains typical mojibake patterns (Ã followed by another char)
            if (text.contains("\u00C3") || text.contains("\u00C2")) {
                byte[] latin1Bytes = text.getBytes(java.nio.charset.StandardCharsets.ISO_8859_1);
                String fixed = new String(latin1Bytes, java.nio.charset.StandardCharsets.UTF_8);
                // Verify the re-encoding produced valid text (no replacement chars)
                if (!fixed.contains("\uFFFD")) {
                    return fixed;
                }
            }
        } catch (Exception ignored) {}
        return text;
    }

    /** Release all resources. */
    public void dispose() {
        stopRecording();
        if (model != null) {
            try { model.close(); } catch (Exception ignored) {}
            model = null;
        }
    }
}
