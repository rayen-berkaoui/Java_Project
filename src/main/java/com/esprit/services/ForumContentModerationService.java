package com.esprit.services;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ForumContentModerationService {

    private static final String DEFAULT_BADWORDS_PATH = "/moderation/badwords.txt";
    private static final String CONFIG_PATH = "/moderation/moderation.properties";

    private final Set<String> badWords = new HashSet<>();
    private final boolean useApiFallback;

    public ForumContentModerationService() {
        loadBadWords();
        useApiFallback = loadApiFallbackConfig();
    }

    private void loadBadWords() {
        try (InputStream is = getClass().getResourceAsStream(DEFAULT_BADWORDS_PATH)) {
            if (is != null) {
                try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        String w = line.trim().toLowerCase();
                        if (!w.isEmpty() && !w.startsWith("#")) {
                            badWords.add(w);
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Modération: impossible de charger badwords.txt, utilisation liste par défaut");
        }
        if (badWords.isEmpty()) {
            badWords.addAll(Arrays.asList(
                    "merde", "pute", "putain", "connard", "connasse", "enculé", "enculer",
                    "salaud", "salope", "nique", "niquer", "bordel", "con", "conne",
                    "fuck", "shit", "damn", "bitch", "ass", "dick", "crap", "hell"
            ));
        }
    }

    private boolean loadApiFallbackConfig() {
        try (InputStream is = getClass().getResourceAsStream(CONFIG_PATH)) {
            if (is != null) {
                Properties p = new Properties();
                p.load(is);
                return "true".equalsIgnoreCase(p.getProperty("moderation.use.api.fallback", "false"));
            }
        } catch (Exception ignored) {}
        return false;
    }

    private String normalizeLeet(String text) {
        if (text == null) return "";
        String s = text.toLowerCase()
                .replace("4", "a").replace("@", "a").replace("å", "a").replace("á", "a")
                .replace("0", "o").replace("ø", "o").replace("ó", "o")
                .replace("1", "i").replace("!", "i").replace("|", "i")
                .replace("3", "e").replace("é", "e").replace("è", "e").replace("ê", "e")
                .replace("$", "s").replace("5", "s")
                .replace("7", "t")
                .replace("8", "b")
                .replace("9", "g")
                .replace("+", "t")
                .replace("(", "c").replace("<", "c")
                .replace(")", "c")
                .replace("€", "e")
                .replace("§", "s");
        return s;
    }

    private String removeObfuscation(String text) {
        if (text == null) return "";
        String s = text.replaceAll("[\\s.\\-_*]+", "");
        return s.replaceAll("(.)\\1{2,}", "$1$1");
    }

    private List<String> extractWords(String normalized) {
        List<String> words = new ArrayList<>();
        Matcher m = Pattern.compile("[a-zàâäéèêëïîôùûüç]+").matcher(normalized);
        while (m.find()) {
            words.add(m.group());
        }
        return words;
    }

    public boolean containsInappropriateContent(String text) {
        return findFirstBadWord(text) != null;
    }

    public String findFirstBadWord(String text) {
        if (text == null || text.isBlank()) return null;

        String normalized = normalizeLeet(text);
        String deobfuscated = removeObfuscation(normalized);
        List<String> words = extractWords(deobfuscated);

        for (String word : words) {
            if (word.length() < 2) continue;
            if (badWords.contains(word)) return word;
            for (String bad : badWords) {
                if (bad.length() >= 5 && word.length() > bad.length() && word.contains(bad)) return bad;
            }
        }
        return null;
    }

    private boolean checkWithApi(String text) {
        try {
            String encoded = URLEncoder.encode(text, StandardCharsets.UTF_8);
            URI uri = URI.create("https://www.purgomalum.com/service/containsprofanity?text=" + encoded);
            HttpURLConnection conn = (HttpURLConnection) uri.toURL().openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(3000);
            if (conn.getResponseCode() == 200) {
                try (BufferedReader r = new BufferedReader(
                        new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                    String line = r.readLine();
                    return "true".equalsIgnoreCase(line != null ? line.trim() : "");
                }
            }
        } catch (Exception e) {
            System.err.println("Modération API: " + e.getMessage());
        }
        return false;
    }

    public ModerationResult checkPost(String textContent) {
        if (textContent == null || textContent.isBlank()) {
            return new ModerationResult(true, null, null);
        }

        String localMatch = findFirstBadWord(textContent);
        if (localMatch != null) {
            return new ModerationResult(false,
                    "Contenu non autorisé. Mot détecté : " + localMatch,
                    localMatch);
        }

        String apiDetected = checkWithPurgoMalumDetailed(textContent);
        if (apiDetected != null) {
            return new ModerationResult(false,
                    "Contenu détecté comme inapproprié par l'API de modération.",
                    apiDetected);
        }

        return new ModerationResult(true, null, null);
    }

    private String checkWithPurgoMalumDetailed(String text) {
        try {
            String encoded = URLEncoder.encode(text, StandardCharsets.UTF_8);
            URI uri = URI.create("https://www.purgomalum.com/service/json?text=" + encoded);
            HttpURLConnection conn = (HttpURLConnection) uri.toURL().openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            if (conn.getResponseCode() == 200) {
                try (BufferedReader r = new BufferedReader(
                        new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = r.readLine()) != null) sb.append(line);
                    String json = sb.toString();

                    int idx = json.indexOf("\"result\":");
                    if (idx >= 0) {
                        int start = json.indexOf('"', idx + 9) + 1;
                        int end = json.lastIndexOf('"');
                        if (start > 0 && end > start) {
                            String filtered = json.substring(start, end);
                            if (!filtered.equals(text) && filtered.contains("*")) {

                                return extractReplacedWord(text, filtered);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[Modération API PurgoMalum] " + e.getMessage());
        }
        return null;
    }

    private String extractReplacedWord(String original, String filtered) {
        String[] origWords = original.split("\\s+");
        String[] filtWords = filtered.split("\\s+");
        for (int i = 0; i < Math.min(origWords.length, filtWords.length); i++) {
            if (!origWords[i].equals(filtWords[i]) && filtWords[i].contains("*")) {
                return origWords[i];
            }
        }
        return "[API detected]";
    }

    public static class ModerationResult {
        private final boolean allowed;
        private final String rejectionReason;
        private final String matchedWord;

        public ModerationResult(boolean allowed, String rejectionReason, String matchedWord) {
            this.allowed = allowed;
            this.rejectionReason = rejectionReason;
            this.matchedWord = matchedWord;
        }

        public boolean isAllowed() { return allowed; }
        public String getRejectionReason() { return rejectionReason; }
        public String getMatchedWord() { return matchedWord; }
    }
}


