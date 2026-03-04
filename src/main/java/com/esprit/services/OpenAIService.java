 package com.esprit.services;

import com.esprit.entities.Activite;
import com.esprit.entities.Etablissement;
import com.google.gson.*;
import okhttp3.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

/**
 * Service that communicates with the OpenAI Chat Completions API
 * to power an intelligent travel assistant chatbot.
 *
 * The service injects real database data (establishments & activities)
 * into the system prompt so the AI can provide contextual, accurate answers.
 */
public class OpenAIService {

    // ─── Configuration ───────────────────────────────────────────────
    private static final String API_KEY;
    private static final String MODEL;

    static {
        String key = "YOUR_OPENAI_API_KEY";
        String model = "gpt-4o-mini";
        try (InputStream in = OpenAIService.class.getResourceAsStream("/config.properties")) {
            if (in != null) {
                Properties props = new Properties();
                props.load(in);
                key = props.getProperty("openai.api.key", key);
                model = props.getProperty("openai.model", model);
            }
        } catch (IOException e) { /* keep defaults */ }
        API_KEY = key;
        MODEL = model;
    }

    private static final String API_URL = "https://api.openai.com/v1/chat/completions";

    private static final int MAX_HISTORY = 20; // max conversation turns to keep

    private final OkHttpClient httpClient;
    private final Gson gson = new Gson();

    // Conversation history for multi-turn context
    private final List<JsonObject> conversationHistory = new ArrayList<>();

    // System prompt (rebuilt when data changes)
    private String systemPrompt = "";

    public OpenAIService() {
        httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    /**
     * Check if the API key has been configured.
     */
    public boolean isConfigured() {
        return API_KEY != null
                && !API_KEY.isBlank()
                && !API_KEY.equals("YOUR_OPENAI_API_KEY");
    }

    /**
     * Rebuild the system prompt with fresh database context.
     * Should be called whenever data is refreshed.
     */
    public void updateContext(List<Etablissement> etablissements, List<Activite> activites, String lang) {
        StringBuilder sb = new StringBuilder();

        sb.append("Tu es un assistant de voyage intelligent pour une agence de voyage tunisienne. ");
        sb.append("Tu aides les utilisateurs à découvrir des établissements (hôtels, restaurants, cafés, musées, bars) ");
        sb.append("et des activités touristiques. Tu es chaleureux, professionnel et concis.\n\n");

        sb.append("RÈGLES IMPORTANTES :\n");
        sb.append("- Réponds dans la langue de l'utilisateur (FR, EN ou AR selon le contexte).\n");
        sb.append("- La langue actuelle est : ").append(lang).append(".\n");
        sb.append("- Base tes recommandations UNIQUEMENT sur les données réelles ci-dessous.\n");
        sb.append("- Si l'utilisateur demande quelque chose hors de ta base de données, dis-le poliment.\n");
        sb.append("- Utilise des emojis pour rendre les réponses attractives.\n");
        sb.append("- Formate les noms importants en **gras** (Markdown).\n");
        sb.append("- Sois capable de : recommander, filtrer par ville/type/catégorie/budget/niveau, ");
        sb.append("générer des plannings, donner des stats, et avoir des conversations naturelles.\n");
        sb.append("- Pour les prix, utilise la devise TND (Dinar Tunisien) sauf indication contraire.\n\n");

        // ── Inject establishments data ──
        sb.append("=== ÉTABLISSEMENTS DISPONIBLES (").append(etablissements.size()).append(") ===\n");
        int count = 0;
        for (Etablissement e : etablissements) {
            if (count++ >= 50) { // limit to avoid token overflow
                sb.append("... et ").append(etablissements.size() - 50).append(" autres établissements.\n");
                break;
            }
            sb.append("- ").append(safe(e.getNom()));
            if (e.getType() != null) sb.append(" [").append(e.getType()).append("]");
            if (e.getVille() != null) sb.append(" à ").append(e.getVille());
            if (e.getAdresse() != null) sb.append(", ").append(e.getAdresse());
            if (e.getGammePrix() != null && !e.getGammePrix().isBlank()) sb.append(" (").append(e.getGammePrix()).append(")");
            if (e.getTelephone() != null && !e.getTelephone().isBlank()) sb.append(" Tél:").append(e.getTelephone());
            if (e.getHoraires() != null && !e.getHoraires().isBlank()) sb.append(" Horaires:").append(e.getHoraires());
            if (e.getDescription() != null && !e.getDescription().isBlank()) {
                String desc = e.getDescription().length() > 100 ? e.getDescription().substring(0, 100) + "..." : e.getDescription();
                sb.append(" — ").append(desc);
            }
            sb.append("\n");
        }

        // ── Inject activities data ──
        sb.append("\n=== ACTIVITÉS DISPONIBLES (").append(activites.size()).append(") ===\n");
        count = 0;
        for (Activite a : activites) {
            if (count++ >= 50) {
                sb.append("... et ").append(activites.size() - 50).append(" autres activités.\n");
                break;
            }
            sb.append("- ").append(safe(a.getNomActivite()));
            if (a.getCategorie() != null) sb.append(" [").append(a.getCategorie()).append("]");
            if (a.getNiveau() != null) sb.append(" Niveau:").append(a.getNiveau());
            if (a.getPrix() != null) sb.append(" Prix:").append(a.getPrix()).append(" ").append(safe(a.getDevise()));
            if (a.getDuree() != null) sb.append(" Durée:").append(a.getDuree()).append("min");
            if (a.getStatut() != null) sb.append(" (").append(a.getStatut()).append(")");
            if (a.getPlacesDispo() != null && a.getNbPlaces() != null)
                sb.append(" Places:").append(a.getPlacesDispo()).append("/").append(a.getNbPlaces());
            if (a.getAgeMin() != null) sb.append(" AgeMin:").append(a.getAgeMin());
            if (a.getDescription() != null && !a.getDescription().isBlank()) {
                String desc = a.getDescription().length() > 100 ? a.getDescription().substring(0, 100) + "..." : a.getDescription();
                sb.append(" — ").append(desc);
            }
            sb.append("\n");
        }

        systemPrompt = sb.toString();

        // Reset conversation when context changes
        conversationHistory.clear();
    }

    /**
     * Send a user message to OpenAI and return the assistant's response.
     *
     * @param userMessage the user's input text
     * @return AI-generated response text
     * @throws IOException if the API call fails
     */
    public String chat(String userMessage) throws IOException {
        if (!isConfigured()) {
            throw new IOException("Clé API OpenAI non configurée. Mettez votre clé dans OpenAIService.java");
        }

        // Add user message to history
        JsonObject userMsg = new JsonObject();
        userMsg.addProperty("role", "user");
        userMsg.addProperty("content", userMessage);
        conversationHistory.add(userMsg);

        // Trim history if too long
        while (conversationHistory.size() > MAX_HISTORY) {
            conversationHistory.remove(0);
        }

        // Build the messages array
        JsonArray messages = new JsonArray();

        // System prompt
        JsonObject sysMsg = new JsonObject();
        sysMsg.addProperty("role", "system");
        sysMsg.addProperty("content", systemPrompt);
        messages.add(sysMsg);

        // Conversation history
        for (JsonObject msg : conversationHistory) {
            messages.add(msg);
        }

        // Build request body
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("model", MODEL);
        requestBody.add("messages", messages);
        requestBody.addProperty("max_tokens", 1000);
        requestBody.addProperty("temperature", 0.7);

        RequestBody body = RequestBody.create(
                gson.toJson(requestBody),
                MediaType.parse("application/json; charset=utf-8")
        );

        Request request = new Request.Builder()
                .url(API_URL)
                .addHeader("Authorization", "Bearer " + API_KEY)
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            String responseBody = response.body() != null ? response.body().string() : "";

            if (!response.isSuccessful()) {
                // Parse error message from OpenAI
                String errorMsg = "Erreur API OpenAI (HTTP " + response.code() + ")";
                try {
                    JsonObject errorJson = JsonParser.parseString(responseBody).getAsJsonObject();
                    if (errorJson.has("error")) {
                        JsonObject error = errorJson.getAsJsonObject("error");
                        if (error.has("message")) {
                            errorMsg = error.get("message").getAsString();
                        }
                    }
                } catch (Exception ignored) {}
                throw new IOException(errorMsg);
            }

            // Parse assistant response
            JsonObject json = JsonParser.parseString(responseBody).getAsJsonObject();
            JsonArray choices = json.getAsJsonArray("choices");
            if (choices != null && choices.size() > 0) {
                JsonObject firstChoice = choices.get(0).getAsJsonObject();
                JsonObject message = firstChoice.getAsJsonObject("message");
                String assistantContent = message.get("content").getAsString();

                // Add assistant response to history
                JsonObject assistantMsg = new JsonObject();
                assistantMsg.addProperty("role", "assistant");
                assistantMsg.addProperty("content", assistantContent);
                conversationHistory.add(assistantMsg);

                return assistantContent;
            }

            throw new IOException("Réponse vide de l'API OpenAI");
        }
    }

    /**
     * Clear conversation history (for starting a new conversation).
     */
    public void clearHistory() {
        conversationHistory.clear();
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }
}
