package com.esprit.services;

import com.esprit.entities.Panier;
import com.esprit.entities.Reservation;

import java.util.*;
import java.util.stream.Collectors;

/**
 * AI-Powered Smart Features Service.
 * Provides intelligent analytics, predictions, and recommendations
 * for panier and reservation management.
 */
public class SmartAIService {

    private final ChatbotService chatbot = new ChatbotService();

    /**
     * Analyze user's booking patterns and generate smart insights
     */
    public String analyzeBookingPatterns(List<Reservation> reservations, List<Panier> panierItems) {
        if (reservations.isEmpty() && panierItems.isEmpty()) {
            return "Pas encore de donnees. Commencez a reserver pour recevoir des recommandations personnalisees !";
        }

        StringBuilder context = new StringBuilder();
        context.append("Analyse le profil voyageur:\n");
        
        // Count service types
        Map<String, Long> serviceCount = new HashMap<>();
        for (Reservation r : reservations) {
            String type = r.getTypeService() != null ? r.getTypeService() : "Autre";
            serviceCount.merge(type, 1L, Long::sum);
        }
        for (Panier p : panierItems) {
            String type = p.getTypeService() != null ? p.getTypeService() : "Autre";
            serviceCount.merge(type, 1L, Long::sum);
        }
        
        context.append("- Services utilises: ").append(serviceCount).append("\n");
        
        // Total spent
        double totalSpent = reservations.stream().mapToDouble(Reservation::getMontantTotal).sum();
        context.append("- Total depense: ").append(String.format("%.2f DT", totalSpent)).append("\n");
        
        // Destinations
        Set<String> destinations = new HashSet<>();
        for (Reservation r : reservations) {
            if (r.getNomEtablissement() != null) destinations.add(r.getNomEtablissement());
        }
        for (Panier p : panierItems) {
            if (p.getNomEtablissement() != null) destinations.add(p.getNomEtablissement());
        }
        context.append("- Etablissements visites: ").append(destinations).append("\n");
        
        context.append("Donne 3 recommandations personnalisees basees sur ce profil. Format: emoji + titre + 1 phrase.");
        
        return chatbot.chat(context.toString());
    }

    /**
     * Generate a smart budget estimate for a trip
     */
    public String estimateBudget(String destination, int nights, int persons, String serviceType) {
        String prompt = String.format(
            "Estime le budget pour %d personne(s), %d nuit(s) a %s (%s). " +
            "Inclus: hebergement, repas, transport, activites. " +
            "Format: tableau avec min/moyen/confort. En DT (Dinar Tunisien).",
            persons, nights, destination, serviceType
        );
        return chatbot.chat(prompt);
    }

    /**
     * Get AI-powered suggestions based on current panier content
     */
    public String getSmartSuggestions(List<Panier> panierItems) {
        if (panierItems.isEmpty()) {
            return "Votre panier est vide. Explorez nos destinations pour commencer !";
        }

        StringBuilder items = new StringBuilder();
        double total = 0;
        for (Panier p : panierItems) {
            items.append(p.getNomEtablissement() != null ? p.getNomEtablissement() : p.getTypeService());
            items.append(" (").append(String.format("%.0f DT", p.getPrixEstime())).append("), ");
            total += p.getPrixEstime();
        }

        return chatbot.getPanierSuggestions(items.toString(), total);
    }

    /**
     * Sentiment analysis on reservation experience
     */
    public String analyzeAndSuggest(String userFeedback) {
        String prompt = "Un client dit: \"" + userFeedback + "\". " +
            "Analyse son sentiment et propose une action concrete pour ameliorer son experience. " +
            "Sois empathique et professionnel.";
        return chatbot.chat(prompt);
    }

    /**
     * Smart pricing advice
     */
    public String getPricingAdvice(double currentPrice, String serviceType, String location) {
        String prompt = String.format(
            "Le prix actuel pour %s a %s est %.2f DT. " +
            "Est-ce un bon prix ? Conseils pour economiser ? Meilleur moment pour reserver ?",
            serviceType, location, currentPrice
        );
        return chatbot.chat(prompt);
    }

    /**
     * Generate travel itinerary suggestion
     */
    public String generateItinerary(String destination, int days) {
        String prompt = String.format(
            "Cree un itineraire de %d jour(s) a %s. " +
            "Format: Jour X: matin/apres-midi/soir avec activites. " +
            "Inclus des restaurants et points d'interet. Concis.",
            days, destination
        );
        return chatbot.chat(prompt);
    }
}
