package com.esprit.services;

import java.util.concurrent.CompletableFuture;

/**
 * AI description generator using Google Gemini API.
 * Delegates to GeminiService for all AI calls.
 */
public class DescriptionGeneratorService {

    private final GeminiService geminiService = new GeminiService();

    public boolean isConfigured() {
        // Gemini key is always loaded from gemini.properties
        return true;
    }

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

        return callGemini(prompt.toString());
    }

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

        return callGemini(prompt.toString());
    }

    private CompletableFuture<String> callGemini(String userPrompt) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return geminiService.generateText(userPrompt);
            } catch (Exception e) {
                throw new RuntimeException(e.getMessage(), e);
            }
        });
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }
}
