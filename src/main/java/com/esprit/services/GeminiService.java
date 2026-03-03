package com.esprit.services;

import com.google.gson.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for interacting with Google Gemini AI API.
 * Used for the tourism chatbot and AI description generation.
 */
public class GeminiService {

    private static final String API_KEY = "AIzaSyC16bP-UfJ2fHQCx7A7KnhcLkehO1GF6w0";
    private static final String[] MODELS = {
            "gemini-2.5-flash",
            "gemini-2.0-flash-lite",
            "gemini-2.0-flash"
    };
    private String activeModel = MODELS[0];
    private static final String API_BASE = "https://generativelanguage.googleapis.com/v1beta/models/";

    private final HttpClient httpClient;
    private final Gson gson;

    /** Conversation history for multi-turn chat */
    private final List<JsonObject> conversationHistory = new ArrayList<>();

    public GeminiService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        this.gson = new GsonBuilder().create();
    }

    // ========== Chatbot ==========

    /**
     * Send a chat message with full conversation history for context.
     * The system prompt grounds the AI in the tourism database context.
     *
     * @param userMessage the user's message
     * @param dbContext   summary of the database contents (places, categories, addresses)
     * @return AI response text
     */
    public String chat(String userMessage, String dbContext) throws Exception {
        // Build system instruction
        String systemPrompt = """
                Tu es un assistant IA polyvalent et intelligent intégré dans une application JavaFX de gestion de lieux touristiques en Tunisie.
                Tu parles en français par défaut, mais tu peux répondre dans la langue de l'utilisateur s'il t'écrit dans une autre langue.
                Tu es amical, concis et très utile.
                
                Tu peux répondre à TOUT type de question:
                - Questions sur le tourisme en Tunisie (ta spécialité)
                - Questions générales de culture, science, histoire, géographie, maths, programmation, etc.
                - Aide avec des problèmes, conseils, recommandations
                - Conversations informelles
                
                DONNÉES DE LA BASE TOURISTIQUE (utilise-les pour les questions sur l'app):
                %s
                
                Pour les questions touristiques:
                - Base tes recommandations sur les données réelles de la base quand disponibles
                - Suggère des itinéraires, compare les prix, recommande par catégorie
                - Si un lieu n'existe pas dans la base, dis-le clairement
                
                Règles générales:
                - Utilise des émojis pour rendre tes réponses engageantes
                - Sois concis (3-5 phrases) sauf si on demande des détails
                - Si on te demande de faire quelque chose de nuisible ou inapproprié, refuse poliment
                """.formatted(dbContext);

        // Add user message to history
        JsonObject userContent = new JsonObject();
        userContent.addProperty("role", "user");
        JsonArray userParts = new JsonArray();
        JsonObject userPart = new JsonObject();
        userPart.addProperty("text", userMessage);
        userParts.add(userPart);
        userContent.add("parts", userParts);
        conversationHistory.add(userContent);

        // Build request with system instruction + conversation history
        JsonObject requestBody = new JsonObject();

        // System instruction
        JsonObject systemInstruction = new JsonObject();
        JsonArray sysParts = new JsonArray();
        JsonObject sysPart = new JsonObject();
        sysPart.addProperty("text", systemPrompt);
        sysParts.add(sysPart);
        systemInstruction.add("parts", sysParts);
        requestBody.add("system_instruction", systemInstruction);

        // Contents (conversation history)
        JsonArray contents = new JsonArray();
        for (JsonObject msg : conversationHistory) {
            contents.add(msg);
        }
        requestBody.add("contents", contents);

        // Generation config
        JsonObject genConfig = new JsonObject();
        genConfig.addProperty("temperature", 0.7);
        genConfig.addProperty("maxOutputTokens", 800);
        requestBody.add("generationConfig", genConfig);

        // Send request
        String responseText = sendRequest(requestBody);

        // Add assistant response to history
        JsonObject assistantContent = new JsonObject();
        assistantContent.addProperty("role", "model");
        JsonArray assistantParts = new JsonArray();
        JsonObject assistantPart = new JsonObject();
        assistantPart.addProperty("text", responseText);
        assistantParts.add(assistantPart);
        assistantContent.add("parts", assistantParts);
        conversationHistory.add(assistantContent);

        // Keep history manageable (last 20 messages)
        while (conversationHistory.size() > 20) {
            conversationHistory.remove(0);
        }

        return responseText;
    }

    /** Clear conversation history to start fresh */
    public void clearHistory() {
        conversationHistory.clear();
    }

    // ========== Description Generator ==========

    /**
     * Generate an attractive marketing description for a tourist place.
     *
     * @param placeName    name of the place
     * @param city         city where the place is located
     * @param categoryName category of the place
     * @param price        price in TND
     * @return generated description
     */
    public String generateDescription(String placeName, String city, String categoryName, double price) throws Exception {
        String prompt = """
                Génère une description touristique attrayante et professionnelle en français pour ce lieu:
                
                Nom: %s
                Ville: %s
                Catégorie: %s
                Prix: %.2f TND
                
                La description doit:
                - Faire 2-3 phrases (50-100 mots maximum)
                - Être engageante et donner envie de visiter
                - Mentionner la ville et la catégorie naturellement
                - Être factuelle et réaliste (ne pas inventer de détails spécifiques)
                - Ne pas commencer avec le nom du lieu
                
                Réponds UNIQUEMENT avec la description, sans guillemets ni préfixe.
                """.formatted(placeName, city, categoryName, price);

        JsonObject requestBody = new JsonObject();
        JsonArray contents = new JsonArray();
        JsonObject content = new JsonObject();
        content.addProperty("role", "user");
        JsonArray parts = new JsonArray();
        JsonObject part = new JsonObject();
        part.addProperty("text", prompt);
        parts.add(part);
        content.add("parts", parts);
        contents.add(content);
        requestBody.add("contents", contents);

        JsonObject genConfig = new JsonObject();
        genConfig.addProperty("temperature", 0.8);
        genConfig.addProperty("maxOutputTokens", 200);
        requestBody.add("generationConfig", genConfig);

        return sendRequest(requestBody);
    }

    // ========== HTTP Layer with Model Fallback ==========

    private String getApiUrl(String model) {
        return API_BASE + model + ":generateContent?key=" + API_KEY;
    }

    private String sendRequest(JsonObject requestBody) throws Exception {
        String jsonBody = gson.toJson(requestBody);
        Exception lastError = null;

        // Try each model, fall back on quota/rate errors
        for (String model : MODELS) {
            try {
                String url = getApiUrl(model);
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .timeout(Duration.ofSeconds(30))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    // Parse successful response
                    if (!model.equals(activeModel)) {
                        activeModel = model;
                        System.out.println("🔄 Switched to Gemini model: " + model);
                    }
                    return parseResponse(response.body());
                } else if (response.statusCode() == 429 || response.statusCode() == 403) {
                    // Quota exceeded or forbidden — try next model
                    System.err.println("⚠️ Model " + model + " quota exceeded, trying next...");
                    lastError = new RuntimeException("Quota exceeded for " + model);
                    continue;
                } else {
                    // Other error — extract message
                    String errorMsg = "API Error " + response.statusCode();
                    try {
                        JsonObject errObj = JsonParser.parseString(response.body()).getAsJsonObject();
                        if (errObj.has("error")) {
                            errorMsg = errObj.getAsJsonObject("error").get("message").getAsString();
                            // Check if it's a quota error in the message body
                            if (errorMsg.toLowerCase().contains("quota") || errorMsg.toLowerCase().contains("rate")) {
                                System.err.println("⚠️ Model " + model + ": " + errorMsg);
                                lastError = new RuntimeException(errorMsg);
                                continue;
                            }
                        }
                    } catch (Exception ignored) {}
                    throw new RuntimeException(errorMsg);
                }
            } catch (RuntimeException e) {
                lastError = e;
                if (e.getMessage() != null && (e.getMessage().contains("Quota") || e.getMessage().contains("quota"))) {
                    continue; // Try next model
                }
                throw e;
            }
        }

        // All models exhausted
        throw lastError != null ? lastError
                : new RuntimeException("Tous les modèles Gemini sont indisponibles. Réessayez plus tard.");
    }

    private String parseResponse(String body) {
        JsonObject responseJson = JsonParser.parseString(body).getAsJsonObject();
        JsonArray candidates = responseJson.getAsJsonArray("candidates");
        if (candidates != null && candidates.size() > 0) {
            JsonObject firstCandidate = candidates.get(0).getAsJsonObject();
            JsonObject contentObj = firstCandidate.getAsJsonObject("content");
            if (contentObj != null) {
                JsonArray partsArr = contentObj.getAsJsonArray("parts");
                if (partsArr != null && partsArr.size() > 0) {
                    return partsArr.get(0).getAsJsonObject().get("text").getAsString().trim();
                }
            }
        }
        throw new RuntimeException("Réponse vide de l'IA");
    }
}
