package com.esprit.services;

import javafx.application.Platform;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * Service de traduction automatique multilingue.
 * Utilise l'API Google Translate (endpoint gratuit) pour une qualité optimale.
 * Fallback sur MyMemory si Google ne r\u00e9pond pas.
 * Cache thread-safe pour \u00e9viter les appels r\u00e9seau redondants.
 */
public class TranslationService {

    // Google Translate unofficial endpoint (meilleure qualit\u00e9)
    private static final String GOOGLE_API = "https://translate.googleapis.com/translate_a/single";
    // Fallback MyMemory
    private static final String MYMEMORY_API = "https://api.mymemory.translated.net/get";

    private static final ConcurrentHashMap<String, String> cache = new ConcurrentHashMap<>();
    private static final ExecutorService executor = Executors.newFixedThreadPool(4);

    private static final String SOURCE_LANG = "fr";
    private static volatile String currentLang = "fr";
    private static final List<Runnable> languageChangeListeners = new CopyOnWriteArrayList<>();

    public static String getCurrentLang() { return currentLang; }
    public static void setCurrentLang(String lang) {
        currentLang = lang;
        if (!languageChangeListeners.isEmpty()) {
            Platform.runLater(() -> languageChangeListeners.forEach(Runnable::run));
        }
    }

    public static void addLanguageChangeListener(Runnable listener) { languageChangeListeners.add(listener); }
    public static void removeLanguageChangeListener(Runnable listener) { languageChangeListeners.remove(listener); }

    /**
     * Traduit un texte de mani\u00e8re synchrone via Google Translate, avec fallback MyMemory.
     */
    public static String translate(String text, String toLang) {
        if (text == null || text.isBlank()) return text;
        if (toLang.equals(SOURCE_LANG)) return text;
        // Ne pas traduire les tirets, chiffres seuls, ou textes tr\u00e8s courts non-alpha
        String trimmed = text.trim();
        if (trimmed.equals("\u2014") || trimmed.matches("^[\\d\\s.,/:\\-]+$")) return text;

        String cacheKey = text.trim() + "|" + SOURCE_LANG + "|" + toLang;
        String cached = cache.get(cacheKey);
        if (cached != null) return cached;

        // 1) Essayer Google Translate
        String result = translateViaGoogle(text, SOURCE_LANG, toLang);

        // 2) Fallback MyMemory si Google \u00e9choue
        if (result == null || result.isBlank()) {
            result = translateViaMyMemory(text, SOURCE_LANG, toLang);
        }

        if (result != null && !result.isBlank()) {
            cache.put(cacheKey, result);
            return result;
        }
        return text;
    }

    /**
     * Traduction via Google Translate (endpoint non-officiel, pas de cl\u00e9 API).
     */
    private static String translateViaGoogle(String text, String fromLang, String toLang) {
        try {
            String encoded = URLEncoder.encode(text, StandardCharsets.UTF_8);
            String urlStr = GOOGLE_API
                    + "?client=gtx&sl=" + fromLang
                    + "&tl=" + toLang
                    + "&dt=t&q=" + encoded;

            HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");

            if (conn.getResponseCode() == 200) {
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                reader.close();

                // R\u00e9ponse: [[["translated text","source text",...],...],...] 
                return parseGoogleResponse(sb.toString());
            }
        } catch (Exception e) {
            System.err.println("[TranslationService] Google Translate error: " + e.getMessage());
        }
        return null;
    }

    /**
     * Parse la r\u00e9ponse JSON de Google Translate.
     * Format: [[["trans1","src1",...],["trans2","src2",...],...],...]
     * Extrait uniquement le premier champ (texte traduit) de chaque segment,
     * en suivant la profondeur des crochets pour \u00e9viter les tokens internes.
     */
    private static String parseGoogleResponse(String json) {
        try {
            if (json == null || json.length() < 8) return null;

            // Trouver le d\u00e9but [[["
            int i = 0;
            while (i < json.length() - 3) {
                if (json.charAt(i) == '[' && json.charAt(i + 1) == '[' && json.charAt(i + 2) == '[') break;
                i++;
            }
            if (i >= json.length() - 3) return null;
            i += 3; // pass\u00e9 [[[

            StringBuilder result = new StringBuilder();

            while (i < json.length()) {
                // On attend " au d\u00e9but du texte traduit
                if (json.charAt(i) != '"') break;
                i++; // skip opening "

                // Extraire le texte traduit jusqu'au prochain " non-\u00e9chapp\u00e9
                int start = i;
                while (i < json.length()) {
                    if (json.charAt(i) == '\\') { i += 2; continue; }
                    if (json.charAt(i) == '"') break;
                    i++;
                }
                if (i >= json.length()) break;
                result.append(unescapeJson(json.substring(start, i)));
                i++; // skip closing "

                // Avancer jusqu'\u00e0 la fin de ce segment ]
                // On track la profondeur car il peut y avoir des tableaux imbriqu\u00e9s
                int depth = 1; // on est dans un [
                while (i < json.length() && depth > 0) {
                    char c = json.charAt(i);
                    if (c == '"') {
                        // Sauter les cha\u00eenes imbriqu\u00e9es (ex: "source text", tokens)
                        i++;
                        while (i < json.length()) {
                            if (json.charAt(i) == '\\') { i += 2; continue; }
                            if (json.charAt(i) == '"') break;
                            i++;
                        }
                    } else if (c == '[') {
                        depth++;
                    } else if (c == ']') {
                        depth--;
                    }
                    i++;
                }

                // On a ferm\u00e9 le ] du segment. V\u00e9rifier la suite:
                // Si ",["  \u2192 prochain segment de traduction
                // Si "]]" ou autre \u2192 fin des traductions
                if (i < json.length() && json.charAt(i) == ',') {
                    i++; // skip ,
                    if (i < json.length() && json.charAt(i) == '[') {
                        i++; // skip [ \u2014 maintenant on pointe sur " du segment suivant
                    } else {
                        break; // pas un segment, termin\u00e9
                    }
                } else {
                    break; // ]] ou fin, termin\u00e9
                }
            }

            return result.length() > 0 ? result.toString() : null;
        } catch (Exception e) {
            return null;
        }
    }

    private static int findUnescapedQuote(String s, int from) {
        for (int i = from; i < s.length(); i++) {
            if (s.charAt(i) == '"' && (i == 0 || s.charAt(i - 1) != '\\')) return i;
        }
        return -1;
    }

    /**
     * Fallback: traduction via MyMemory.
     */
    private static String translateViaMyMemory(String text, String fromLang, String toLang) {
        try {
            String encoded = URLEncoder.encode(text, StandardCharsets.UTF_8);
            String langPair = fromLang + "|" + toLang;
            String urlStr = MYMEMORY_API + "?q=" + encoded
                    + "&langpair=" + URLEncoder.encode(langPair, StandardCharsets.UTF_8);

            HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.setRequestProperty("User-Agent", "JavaFX-TravelApp/1.0");

            if (conn.getResponseCode() == 200) {
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                reader.close();

                String key = "\"translatedText\":\"";
                int idx = sb.indexOf(key);
                if (idx >= 0) {
                    int start = idx + key.length();
                    int end = findUnescapedQuote(sb.toString(), start);
                    if (end > start) {
                        return unescapeJson(sb.substring(start, end));
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[TranslationService] MyMemory error: " + e.getMessage());
        }
        return null;
    }

    private static String unescapeJson(String s) {
        return s.replace("\\\"", "\"")
                .replace("\\n", "\n")
                .replace("\\t", "\t")
                .replace("\\u0026", "&")
                .replace("\\u003c", "<")
                .replace("\\u003e", ">")
                .replace("\\u0027", "'")
                .replace("\\\\", "\\");
    }

    // ===== ASYNC METHODS =====

    public static void translateAsync(String text, String toLang, Consumer<String> onResult) {
        if (text == null || text.isBlank()) {
            Platform.runLater(() -> onResult.accept(text));
            return;
        }
        if (toLang.equals(SOURCE_LANG)) {
            Platform.runLater(() -> onResult.accept(text));
            return;
        }

        String cacheKey = text.trim() + "|" + SOURCE_LANG + "|" + toLang;
        String cached = cache.get(cacheKey);
        if (cached != null) {
            Platform.runLater(() -> onResult.accept(cached));
            return;
        }

        CompletableFuture.supplyAsync(() -> translate(text, toLang), executor)
                .thenAccept(result -> Platform.runLater(() -> onResult.accept(result)));
    }

    public static void translateBatchAsync(String[] texts, String toLang, Consumer<String[]> onResult) {
        if (toLang.equals(SOURCE_LANG)) {
            Platform.runLater(() -> onResult.accept(texts));
            return;
        }
        CompletableFuture.supplyAsync(() -> {
            String[] results = new String[texts.length];
            for (int i = 0; i < texts.length; i++) {
                results[i] = translate(texts[i], toLang);
            }
            return results;
        }, executor).thenAccept(results -> Platform.runLater(() -> onResult.accept(results)));
    }

    /**
     * Traduit plusieurs labels en une seule op\u00e9ration asynchrone.
     * Pratique pour les pages de d\u00e9tail.
     */
    public static void translateLabels(javafx.scene.control.Label[] labels, String[] originalTexts, String toLang) {
        if (toLang.equals(SOURCE_LANG)) return;
        for (int i = 0; i < labels.length; i++) {
            final javafx.scene.control.Label lbl = labels[i];
            final String txt = originalTexts[i];
            if (txt == null || txt.isBlank() || txt.equals("\u2014")) continue;
            translateAsync(txt, toLang, lbl::setText);
        }
    }

    public static void clearCache() { cache.clear(); }

    // ===== LANG DISPLAY =====

    public static String langDisplayName(String code) {
        switch (code) {
            case "fr": return "FR - Fran\u00e7ais";
            case "en": return "EN - English";
            case "es": return "ES - Espa\u00f1ol";
            case "de": return "DE - Deutsch";
            case "it": return "IT - Italiano";
            case "ar": return "AR - \u0627\u0644\u0639\u0631\u0628\u064a\u0629";
            default: return code;
        }
    }

    public static String langCode(String displayName) {
        if (displayName == null) return "fr";
        if (displayName.startsWith("FR")) return "fr";
        if (displayName.startsWith("EN")) return "en";
        if (displayName.startsWith("ES")) return "es";
        if (displayName.startsWith("DE")) return "de";
        if (displayName.startsWith("IT")) return "it";
        if (displayName.startsWith("AR")) return "ar";
        return "fr";
    }
}
