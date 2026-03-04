package com.esprit.services;

import com.google.gson.*;
import okhttp3.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Service that uses Groq (free tier) to generate professional
 * descriptions for establishments and activities based on form data.
 * Groq provides fast inference with Llama/Mixtral models — free, no credit card.
 */
public class DescriptionGeneratorService {

    private static final String API_KEY;
    private static final String MODEL;

    static {
        String key = "";
        String model = "llama-3.3-70b-versatile";
        try (InputStream in = DescriptionGeneratorService.class.getResourceAsStream("/config.properties")) {
            if (in != null) {
                Properties props = new Properties();
                props.load(in);
                key = props.getProperty("groq.api.key", key);
                model = props.getProperty("groq.model", model);
            }
        } catch (IOException e) { /* keep defaults */ }
        API_KEY = key;
        MODEL = model;
    }

    private static final String API_URL = "https://api.groq.com/openai/v1/chat/completions";

    private final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(40, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .build();

    private final Gson gson = new Gson();

    public boolean isConfigured() {
        return API_KEY != null
                && !API_KEY.isBlank()
                && !API_KEY.equals("YOUR_GROQ_API_KEY");
    }

    // ─── Establishment description ─────────────────────────────────

    /**
     * Generates a description for an establishment (async, non-blocking).
     *
     * @param name       establishment name
     * @param type       type (hotel, restaurant, cafe, museum…)
     * @param city       city / location
     * @param address    street address
     * @param priceRange price range (e.g. "★★★★")
     * @param hours      opening hours
     * @return CompletableFuture with the generated text
     */
    public CompletableFuture<String> generateEstablishmentDescription(
            String name, String type, String city, String address,
            String priceRange, String hours) {

        StringBuilder prompt = new StringBuilder();
        prompt.append("Generate a short, attractive description in English (maximum 100 words) for the following establishment. ");
        prompt.append("The text should be professional, engaging, and suitable for a tourism platform. ");
        prompt.append("Do NOT invent details that are not provided. Only describe what is given.\n\n");

        prompt.append("Establishment details:\n");
        if (notBlank(name)) prompt.append("- Name: ").append(name).append("\n");
        if (notBlank(type)) prompt.append("- Type: ").append(type).append("\n");
        if (notBlank(city)) prompt.append("- City: ").append(city).append("\n");
        if (notBlank(address)) prompt.append("- Address: ").append(address).append("\n");
        if (notBlank(priceRange)) prompt.append("- Price range: ").append(priceRange).append("\n");
        if (notBlank(hours)) prompt.append("- Hours: ").append(hours).append("\n");

        prompt.append("\nRespond with ONLY the description text, no quotes, no title, no extra commentary.");

        return callGroq(prompt.toString());
    }

    // ─── Activity description ──────────────────────────────────────

    /**
     * Generates a description for an activity (async, non-blocking).
     */
    public CompletableFuture<String> generateActivityDescription(
            String name, String category, String level, String duration,
            String price, String currency, String minAge,
            String equipment, String address, String establishmentName) {

        StringBuilder prompt = new StringBuilder();
        prompt.append("Generate a short, attractive description in English (maximum 100 words) for the following activity. ");
        prompt.append("The text should be professional, engaging, and suitable for a tourism platform. ");
        prompt.append("Do NOT invent details that are not provided. Only describe what is given.\n\n");

        prompt.append("Activity details:\n");
        if (notBlank(name)) prompt.append("- Name: ").append(name).append("\n");
        if (notBlank(category)) prompt.append("- Category: ").append(category).append("\n");
        if (notBlank(level)) prompt.append("- Level: ").append(level).append("\n");
        if (notBlank(duration)) prompt.append("- Duration: ").append(duration).append(" minutes\n");
        if (notBlank(price) && notBlank(currency))
            prompt.append("- Price: ").append(price).append(" ").append(currency).append("\n");
        if (notBlank(minAge)) prompt.append("- Minimum age: ").append(minAge).append("\n");
        if (notBlank(equipment)) prompt.append("- Equipment included: ").append(equipment).append("\n");
        if (notBlank(address)) prompt.append("- Location: ").append(address).append("\n");
        if (notBlank(establishmentName))
            prompt.append("- Organized by: ").append(establishmentName).append("\n");

        prompt.append("\nRespond with ONLY the description text, no quotes, no title, no extra commentary.");

        return callGroq(prompt.toString());
    }

    // ─── Core Groq API call (OpenAI-compatible) ────────────────────

    private CompletableFuture<String> callGroq(String userPrompt) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (!isConfigured()) {
                    throw new IOException("Groq API key not configured. Set groq.api.key in config.properties");
                }

                JsonArray messages = new JsonArray();

                JsonObject sysMsg = new JsonObject();
                sysMsg.addProperty("role", "system");
                sysMsg.addProperty("content",
                        "You are a professional copywriter for a tourism platform. " +
                        "You write clear, engaging, and accurate descriptions. " +
                        "Always write in English. Keep the text under 100 words maximum. " +
                        "Never invent information that is not provided.");
                messages.add(sysMsg);

                JsonObject userMsg = new JsonObject();
                userMsg.addProperty("role", "user");
                userMsg.addProperty("content", userPrompt);
                messages.add(userMsg);

                JsonObject body = new JsonObject();
                body.addProperty("model", MODEL);
                body.add("messages", messages);
                body.addProperty("max_tokens", 300);
                body.addProperty("temperature", 0.7);

                RequestBody requestBody = RequestBody.create(
                        gson.toJson(body),
                        MediaType.parse("application/json; charset=utf-8")
                );

                Request request = new Request.Builder()
                        .url(API_URL)
                        .addHeader("Authorization", "Bearer " + API_KEY)
                        .addHeader("Content-Type", "application/json")
                        .post(requestBody)
                        .build();

                try (Response response = httpClient.newCall(request).execute()) {
                    String responseBody = response.body() != null ? response.body().string() : "";

                    if (!response.isSuccessful()) {
                        String errorMsg = "Groq API error (HTTP " + response.code() + ")";
                        try {
                            JsonObject errorJson = JsonParser.parseString(responseBody).getAsJsonObject();
                            if (errorJson.has("error")) {
                                errorMsg = errorJson.getAsJsonObject("error")
                                        .get("message").getAsString();
                            }
                        } catch (Exception ignored) {}
                        throw new IOException(errorMsg);
                    }

                    JsonObject json = JsonParser.parseString(responseBody).getAsJsonObject();
                    JsonArray choices = json.getAsJsonArray("choices");
                    if (choices != null && choices.size() > 0) {
                        return choices.get(0).getAsJsonObject()
                                .getAsJsonObject("message")
                                .get("content").getAsString().trim();
                    }
                    throw new IOException("Empty response from Groq");
                }
            } catch (IOException e) {
                throw new RuntimeException(e.getMessage(), e);
            }
        });
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }
}
