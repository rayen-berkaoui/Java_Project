package com.esprit.services;

import com.esprit.entities.Activite;
import com.esprit.entities.Etablissement;
import com.esprit.services.GooglePlacesService.PlaceResult;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Intelligent chatbot service for the travel agency.
 * Parses natural-language queries, extracts intent + entities,
 * queries the database, and produces formatted responses.
 * Supports FR / EN / AR.
 */
public class ChatbotService {

    private final EtablissementServices etabService = new EtablissementServices();
    private final ActiviteServices actService = new ActiviteServices();
    private final OpenAIService openAIService = new OpenAIService();
    private final GooglePlacesService placesService = new GooglePlacesService();

    // ─── Cached data (refreshed per session) ───
    private List<Etablissement> allEtablissements = Collections.emptyList();
    private List<Activite> allActivites = Collections.emptyList();

    // ─── Language ───
    public enum Lang { FR, EN, AR }
    private Lang currentLang = Lang.FR;

    public void setLang(Lang lang) {
        this.currentLang = lang;
        // Update OpenAI context with new language
        if (aiMode) {
            String langStr = switch (lang) { case FR -> "Français"; case EN -> "English"; case AR -> "العربية"; };
            openAIService.updateContext(allEtablissements, allActivites, langStr);
        }
    }
    public Lang getLang() { return currentLang; }

    // ─── AI Mode ───
    private boolean aiMode = false;

    public boolean isAiMode() { return aiMode; }

    /**
     * Toggle AI mode on/off. When enabled, messages are sent to OpenAI.
     * When disabled, the local keyword-based engine is used.
     * @return true if AI mode is now ON, false if OFF
     */
    public boolean toggleAiMode() {
        aiMode = !aiMode;
        if (aiMode) {
            String langStr = switch (currentLang) { case FR -> "Français"; case EN -> "English"; case AR -> "العربية"; };
            openAIService.updateContext(allEtablissements, allActivites, langStr);
        }
        return aiMode;
    }

    /**
     * @return true if the OpenAI API key is configured
     */
    public boolean isAiConfigured() {
        return openAIService.isConfigured();
    }

    /**
     * @return true if the Google Places API key is configured
     */
    public boolean isPlacesConfigured() {
        return placesService.isConfigured();
    }

    // ─── Intent ───
    public enum Intent {
        SEARCH_ETABLISSEMENT, SEARCH_ACTIVITE,
        FILTER_BY_CITY, FILTER_BY_TYPE, FILTER_BY_CATEGORY,
        FILTER_BY_BUDGET, FILTER_BY_LEVEL,
        RECOMMEND, NEARBY_PLACES, PLANNING,
        STATS, HELP, GREETING, UNKNOWN
    }

    /** Parsed user query */
    public static class ParsedQuery {
        public Intent intent = Intent.UNKNOWN;
        public String city;
        public String type;         // hotel, restaurant, cafe, museum, bar
        public String category;     // sport, culture, nature, gastronomie, loisir
        public String level;        // débutant, intermédiaire, avancé
        public BigDecimal maxBudget;
        public String rawQuery;
        // Nearby / proximity
        public double refLat = Double.NaN;
        public double refLng = Double.NaN;
        public String nearbyType;   // Google place type for nearby search
        public String refEtabName;  // name of the reference establishment ("near the hotel X")
    }

    /** A chatbot response */
    public static class ChatResponse {
        public String text;
        public List<Etablissement> etablissements = new ArrayList<>();
        public List<Activite> activites = new ArrayList<>();
        public List<PlaceResult> nearbyPlaces = new ArrayList<>(); // Google Places results
        public String navigateTo; // optional FXML path to open
        public Map<String, String> filterHints = new LinkedHashMap<>(); // filter key→value for UI

        public ChatResponse(String text) { this.text = text; }
    }

    // ═══════════════════════════════════════════
    //  PUBLIC API
    // ═══════════════════════════════════════════

    public void refreshData() {
        try { allEtablissements = etabService.afficher(); } catch (SQLException e) { allEtablissements = Collections.emptyList(); }
        try { allActivites = actService.afficher(); } catch (SQLException e) { allActivites = Collections.emptyList(); }
        // Update OpenAI context with fresh data
        if (aiMode) {
            String langStr = switch (currentLang) { case FR -> "Français"; case EN -> "English"; case AR -> "العربية"; };
            openAIService.updateContext(allEtablissements, allActivites, langStr);
        }
    }

    /** Main entry: process a user message and return a response */
    public ChatResponse process(String userMessage) {
        if (allEtablissements.isEmpty() && allActivites.isEmpty()) refreshData();

        // ── AI Mode: send to OpenAI ──
        if (aiMode) {
            return processWithAI(userMessage);
        }

        // ── Local Mode: keyword-based engine ──
        return processLocally(userMessage);
    }

    /**
     * Process message using OpenAI API.
     * Falls back to local engine on error.
     */
    private ChatResponse processWithAI(String userMessage) {
        try {
            String aiResponse = openAIService.chat(userMessage);
            return new ChatResponse(aiResponse);
        } catch (Exception e) {
            System.err.println("OpenAI error: " + e.getMessage());
            // Fall back to local engine
            ChatResponse fallbackResp = processLocally(userMessage);
            String prefix = t(
                    "⚠️ _Mode IA indisponible, réponse locale :_\n\n",
                    "⚠️ _AI mode unavailable, local response:_\n\n",
                    "⚠️ _وضع الذكاء الاصطناعي غير متاح، رد محلي:_\n\n"
            );
            fallbackResp.text = prefix + fallbackResp.text;
            return fallbackResp;
        }
    }

    /** Process using the local keyword-based engine */
    private ChatResponse processLocally(String userMessage) {
        ParsedQuery pq = parse(userMessage);

        return switch (pq.intent) {
            case GREETING           -> greeting();
            case HELP               -> help();
            case STATS              -> stats();
            case SEARCH_ETABLISSEMENT -> searchEtablissements(pq);
            case SEARCH_ACTIVITE    -> searchActivites(pq);
            case FILTER_BY_CITY     -> filterByCity(pq);
            case FILTER_BY_TYPE     -> filterByType(pq);
            case FILTER_BY_CATEGORY -> filterByCategory(pq);
            case FILTER_BY_BUDGET   -> filterByBudget(pq);
            case FILTER_BY_LEVEL    -> filterByLevel(pq);
            case RECOMMEND          -> recommend(pq);
            case NEARBY_PLACES      -> nearbyPlaces(pq);
            case PLANNING           -> generatePlanning(pq);
            case UNKNOWN            -> fallback(pq);
        };
    }

    // ═══════════════════════════════════════════
    //  NLP-LIKE PARSER
    // ═══════════════════════════════════════════

    private ParsedQuery parse(String raw) {
        ParsedQuery pq = new ParsedQuery();
        pq.rawQuery = raw;
        String q = raw.toLowerCase().trim();

        // ── Greeting ──
        if (matches(q, "bonjour", "salut", "hello", "hi", "hey", "bonsoir",
                "مرحبا", "سلام", "coucou", "yo")) {
            pq.intent = Intent.GREETING; return pq;
        }

        // ── Help ──
        if (matches(q, "aide", "help", "مساعدة", "commands", "commande",
                "que peux", "what can", "ماذا يمكن")) {
            pq.intent = Intent.HELP; return pq;
        }

        // ── Stats ──
        if (matches(q, "stat", "statistique", "combien", "nombre",
                "how many", "count", "إحصائ", "عدد", "كم")) {
            pq.intent = Intent.STATS; return pq;
        }

        // ── Planning ──
        if (matches(q, "planning", "programme", "itinér", "itiner",
                "journée", "journee", "schedule", "plan my", "برنامج", "خطة")) {
            pq.intent = Intent.PLANNING;
            extractEntities(q, pq);
            return pq;
        }

        // ── Recommend ──
        if (matches(q, "recommand", "suggest", "conseil", "meilleur",
                "best", "top", "أفضل", "أنصح", "propose")) {
            pq.intent = Intent.RECOMMEND;
            extractEntities(q, pq);
            extractNearbyContext(q, pq);
            return pq;
        }

        // ── Nearby / Proximity ──
        if (matches(q, "proche", "autour", "around", "nearby", "near",
                "à côté", "a cote", "à proximité", "a proximite",
                "pas loin", "in the area", "قريب", "حول", "بالقرب")) {
            pq.intent = Intent.NEARBY_PLACES;
            extractEntities(q, pq);
            extractNearbyContext(q, pq);
            return pq;
        }

        // ── Extract entities first ──
        extractEntities(q, pq);

        // ── Budget filter ──
        if (pq.maxBudget != null) {
            if (containsAny(q, "activit", "activity", "نشاط")) {
                pq.intent = Intent.SEARCH_ACTIVITE;
            } else {
                pq.intent = Intent.FILTER_BY_BUDGET;
            }
            return pq;
        }

        // ── Level filter ──
        if (pq.level != null) {
            pq.intent = Intent.FILTER_BY_LEVEL;
            return pq;
        }

        // ── Category (activité) ──
        if (pq.category != null) {
            pq.intent = Intent.FILTER_BY_CATEGORY;
            return pq;
        }

        // ── Type (établissement) ──
        if (pq.type != null && pq.city != null) {
            pq.intent = Intent.SEARCH_ETABLISSEMENT;
            return pq;
        }
        if (pq.type != null) {
            pq.intent = Intent.FILTER_BY_TYPE;
            return pq;
        }

        // ── City ──
        if (pq.city != null) {
            pq.intent = Intent.FILTER_BY_CITY;
            return pq;
        }

        // ── Keyword-based intent ──
        if (containsAny(q, "restaurant", "hotel", "hôtel", "cafe", "café",
                "museum", "musée", "bar", "établissement", "etablissement",
                "مطعم", "فندق", "مقهى", "متحف")) {
            pq.intent = Intent.SEARCH_ETABLISSEMENT;
            return pq;
        }
        if (containsAny(q, "activit", "sport", "culture", "nature",
                "loisir", "aventure", "gastronom", "نشاط", "رياضة")) {
            pq.intent = Intent.SEARCH_ACTIVITE;
            return pq;
        }

        pq.intent = Intent.UNKNOWN;
        return pq;
    }

    private void extractEntities(String q, ParsedQuery pq) {
        // ── Type ──
        if (containsAny(q, "restaurant", "مطعم"))  pq.type = "restaurant";
        else if (containsAny(q, "hotel", "hôtel", "فندق")) pq.type = "hotel";
        else if (containsAny(q, "cafe", "café", "مقهى"))   pq.type = "cafe";
        else if (containsAny(q, "museum", "musée", "متحف")) pq.type = "museum";
        else if (containsAny(q, "bar"))                     pq.type = "bar";

        // ── Category ──
        if (containsAny(q, "sport", "رياضة"))          pq.category = "sport";
        else if (containsAny(q, "culture", "ثقافة"))     pq.category = "culture";
        else if (containsAny(q, "nature", "طبيعة"))      pq.category = "nature";
        else if (containsAny(q, "gastronom", "طعام"))    pq.category = "gastronomie";
        else if (containsAny(q, "loisir", "ترفيه"))      pq.category = "loisir";
        else if (containsAny(q, "aventure", "مغامرة"))   pq.category = "aventure";
        else if (containsAny(q, "bien-etre", "bien être", "wellness")) pq.category = "bien-etre";

        // ── Level ──
        if (containsAny(q, "débutant", "debutant", "beginner", "مبتدئ")) pq.level = "débutant";
        else if (containsAny(q, "intermédiaire", "intermediaire", "intermediate", "متوسط")) pq.level = "intermédiaire";
        else if (containsAny(q, "avancé", "avance", "advanced", "متقدم")) pq.level = "avancé";

        // ── Budget ──
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("(\\d+(?:[.,]\\d+)?)\\s*(?:dt|tnd|dinar|€|\\$|eur)")
                .matcher(q);
        if (!m.find()) {
            m = java.util.regex.Pattern
                    .compile("(?:budget|moins de|max|maximum|under|below|أقل من)\\s*(\\d+(?:[.,]\\d+)?)")
                    .matcher(q);
        }
        if (m.find()) {
            try { pq.maxBudget = new BigDecimal(m.group(1).replace(",", ".")); } catch (Exception ignored) {}
        }

        // ── City (match against known cities in DB) ──
        Set<String> knownCities = allEtablissements.stream()
                .map(Etablissement::getVille)
                .filter(v -> v != null && !v.isBlank())
                .map(v -> v.trim().toLowerCase())
                .collect(Collectors.toSet());

        for (String city : knownCities) {
            if (q.contains(city)) {
                pq.city = city;
                break;
            }
        }
    }

    /**
     * Extract nearby/proximity context from the query.
     * Tries to resolve a reference establishment ("near hotel X")
     * and extract its coordinates, or fall back to a city center.
     */
    private void extractNearbyContext(String q, ParsedQuery pq) {
        // Determine the type the user wants to find nearby
        if (pq.type != null) {
            pq.nearbyType = pq.type;
        } else if (containsAny(q, "restaurant", "مطعم"))    pq.nearbyType = "restaurant";
        else if (containsAny(q, "cafe", "café", "مقهى"))     pq.nearbyType = "cafe";
        else if (containsAny(q, "hotel", "hôtel", "فندق"))   pq.nearbyType = "hotel";
        else if (containsAny(q, "museum", "musée", "متحف"))  pq.nearbyType = "museum";
        else if (containsAny(q, "bar"))                       pq.nearbyType = "bar";
        else if (containsAny(q, "pharmacy", "pharmacie"))     pq.nearbyType = "pharmacy";
        else if (containsAny(q, "parc", "park", "حديقة"))    pq.nearbyType = "park";
        else if (containsAny(q, "spa", "hammam"))             pq.nearbyType = "spa";

        // Try to find a reference establishment by name match
        // e.g. "restaurant proche de l'hôtel Abou Nawas"  →  find "Abou Nawas" in DB
        for (Etablissement etab : allEtablissements) {
            if (etab.getLatitude() == null || etab.getLongitude() == null) continue;
            String name = etab.getNom().toLowerCase();
            if (name.length() >= 3 && q.contains(name)) {
                pq.refLat = etab.getLatitude();
                pq.refLng = etab.getLongitude();
                pq.refEtabName = etab.getNom();
                return;
            }
        }

        // Fall back: if a city is detected, use the average coordinates of that city's establishments
        if (pq.city != null) {
            resolveCoordinatesFromCity(pq, pq.city);
            return;
        }

        // Last resort: use the centroid of all geolocated establishments
        OptionalDouble avgLat = allEtablissements.stream()
                .filter(e -> e.getLatitude() != null)
                .mapToDouble(Etablissement::getLatitude).average();
        OptionalDouble avgLng = allEtablissements.stream()
                .filter(e -> e.getLongitude() != null)
                .mapToDouble(Etablissement::getLongitude).average();
        if (avgLat.isPresent() && avgLng.isPresent()) {
            pq.refLat = avgLat.getAsDouble();
            pq.refLng = avgLng.getAsDouble();
        }
    }

    /**
     * Resolve average coordinates for a given city name from DB establishments.
     */
    private void resolveCoordinatesFromCity(ParsedQuery pq, String city) {
        OptionalDouble avgLat = allEtablissements.stream()
                .filter(e -> city.equalsIgnoreCase(safe(e.getVille()).trim()))
                .filter(e -> e.getLatitude() != null)
                .mapToDouble(Etablissement::getLatitude).average();
        OptionalDouble avgLng = allEtablissements.stream()
                .filter(e -> city.equalsIgnoreCase(safe(e.getVille()).trim()))
                .filter(e -> e.getLongitude() != null)
                .mapToDouble(Etablissement::getLongitude).average();
        if (avgLat.isPresent() && avgLng.isPresent()) {
            pq.refLat = avgLat.getAsDouble();
            pq.refLng = avgLng.getAsDouble();
        }
    }

    // ═══════════════════════════════════════════
    //  RESPONSE BUILDERS
    // ═══════════════════════════════════════════

    private ChatResponse greeting() {
        return new ChatResponse(switch (currentLang) {
            case FR -> "👋 Bonjour ! Je suis votre assistant voyage.\n" +
                    "Demandez-moi de chercher un restaurant, une activité, filtrer par ville/budget, " +
                    "ou générer un planning. Tapez **aide** pour voir toutes les commandes.";
            case EN -> "👋 Hello! I'm your travel assistant.\n" +
                    "Ask me to find a restaurant, an activity, filter by city/budget, " +
                    "or generate a schedule. Type **help** to see all commands.";
            case AR -> "👋 مرحبا! أنا مساعدك في السفر.\n" +
                    "اطلب مني البحث عن مطعم أو نشاط أو التصفية حسب المدينة/الميزانية. " +
                    "اكتب **مساعدة** لرؤية جميع الأوامر.";
        });
    }

    private ChatResponse help() {
        return new ChatResponse(switch (currentLang) {
            case FR -> """
                    🤖 **Commandes disponibles :**

                    🔍 **Recherche** : "restaurant à Tunis", "hôtels", "activités sport"
                    🏙️ **Ville** : "que faire à Sousse", "établissements à Sfax"
                    💰 **Budget** : "activités moins de 50 TND", "restaurant budget 30 DT"
                    🎯 **Catégorie** : "activités nature", "sport débutant"
                    ⭐ **Recommandation** : "recommande un restaurant", "meilleur hôtel"
                    📍 **Proximité** : "restaurants proches", "cafés autour de Tunis"
                    📅 **Planning** : "planning à Tunis", "programme sport nature"
                    📊 **Stats** : "combien de restaurants", "statistiques"
                    🌍 **Langues** : français, english, العربية
                    """;
            case EN -> """
                    🤖 **Available commands:**

                    🔍 **Search**: "restaurant in Tunis", "hotels", "sport activities"
                    🏙️ **City**: "what to do in Sousse", "establishments in Sfax"
                    💰 **Budget**: "activities under 50 TND", "restaurant budget 30"
                    🎯 **Category**: "nature activities", "beginner sport"
                    ⭐ **Recommend**: "recommend a restaurant", "best hotel"
                    📍 **Nearby**: "restaurants near hotel X", "cafes around Tunis"
                    📅 **Planning**: "plan in Tunis", "sport nature schedule"
                    📊 **Stats**: "how many restaurants", "statistics"
                    🌍 **Languages**: français, english, العربية
                    """;
            case AR -> """
                    🤖 **الأوامر المتاحة:**

                    🔍 **بحث**: "مطعم في تونس"، "فنادق"، "أنشطة رياضية"
                    🏙️ **مدينة**: "ماذا أفعل في سوسة"
                    💰 **ميزانية**: "أنشطة أقل من 50 دينار"
                    🎯 **فئة**: "أنشطة طبيعة"، "رياضة مبتدئ"
                    ⭐ **توصية**: "أنصحني بمطعم"
                    📍 **قريب**: "مطاعم قريبة من الفندق"، "مقاهي حول تونس"
                    📅 **برنامج**: "برنامج في تونس"
                    📊 **إحصائيات**: "كم عدد المطاعم"
                    """;
        });
    }

    private ChatResponse stats() {
        long totalEtab = allEtablissements.size();
        long totalAct = allActivites.size();

        Map<String, Long> byType = allEtablissements.stream()
                .collect(Collectors.groupingBy(e -> safe(e.getType()).toLowerCase(), Collectors.counting()));
        Map<String, Long> byCat = allActivites.stream()
                .collect(Collectors.groupingBy(a -> safe(a.getCategorie()).toLowerCase(), Collectors.counting()));
        Map<String, Long> byCity = allEtablissements.stream()
                .collect(Collectors.groupingBy(e -> safe(e.getVille()), Collectors.counting()));

        StringBuilder sb = new StringBuilder();
        sb.append(t("📊 **Statistiques globales**\n\n",
                "📊 **Global Statistics**\n\n",
                "📊 **إحصائيات عامة**\n\n"));

        sb.append(t("🏢 Établissements : ", "🏢 Establishments: ", "🏢 المؤسسات: ")).append(totalEtab).append("\n");
        sb.append(t("🎯 Activités : ", "🎯 Activities: ", "🎯 الأنشطة: ")).append(totalAct).append("\n\n");

        sb.append(t("**Par type :**\n", "**By type:**\n", "**حسب النوع:**\n"));
        byType.entrySet().stream().sorted(Map.Entry.<String,Long>comparingByValue().reversed())
                .forEach(e -> sb.append("  • ").append(capitalize(e.getKey())).append(" : ").append(e.getValue()).append("\n"));

        sb.append(t("\n**Par catégorie :**\n", "\n**By category:**\n", "\n**حسب الفئة:**\n"));
        byCat.entrySet().stream().sorted(Map.Entry.<String,Long>comparingByValue().reversed())
                .forEach(e -> sb.append("  • ").append(capitalize(e.getKey())).append(" : ").append(e.getValue()).append("\n"));

        sb.append(t("\n**Par ville :**\n", "\n**By city:**\n", "\n**حسب المدينة:**\n"));
        byCity.entrySet().stream().sorted(Map.Entry.<String,Long>comparingByValue().reversed()).limit(8)
                .forEach(e -> sb.append("  • ").append(e.getKey()).append(" : ").append(e.getValue()).append("\n"));

        return new ChatResponse(sb.toString());
    }

    // ── Établissement searches ──

    private ChatResponse searchEtablissements(ParsedQuery pq) {
        List<Etablissement> results = allEtablissements.stream()
                .filter(e -> pq.type == null || safe(e.getType()).equalsIgnoreCase(pq.type))
                .filter(e -> pq.city == null || safe(e.getVille()).equalsIgnoreCase(pq.city))
                .collect(Collectors.toList());

        if (results.isEmpty()) return noResults(pq);
        return formatEtabResults(results, pq);
    }

    private ChatResponse filterByCity(ParsedQuery pq) {
        List<Etablissement> etabs = allEtablissements.stream()
                .filter(e -> pq.city != null && safe(e.getVille()).equalsIgnoreCase(pq.city))
                .collect(Collectors.toList());
        List<Activite> acts = allActivites.stream()
                .filter(a -> {
                    if (a.getIdEtablissement() == null) return false;
                    return etabs.stream().anyMatch(e -> e.getIdEtablissement() == a.getIdEtablissement());
                })
                .collect(Collectors.toList());

        StringBuilder sb = new StringBuilder();
        sb.append(t("🏙️ Résultats pour **" + capitalize(pq.city) + "** :\n\n",
                "🏙️ Results for **" + capitalize(pq.city) + "**:\n\n",
                "🏙️ نتائج **" + capitalize(pq.city) + "**:\n\n"));

        if (!etabs.isEmpty()) {
            sb.append(t("**Établissements** (", "**Establishments** (", "**المؤسسات** ("))
                    .append(etabs.size()).append(") :\n");
            etabs.stream().limit(5).forEach(e -> sb.append(formatEtabLine(e)));
        }
        if (!acts.isEmpty()) {
            sb.append(t("\n**Activités** (", "\n**Activities** (", "\n**الأنشطة** ("))
                    .append(acts.size()).append(") :\n");
            acts.stream().limit(5).forEach(a -> sb.append(formatActLine(a)));
        }
        if (etabs.isEmpty() && acts.isEmpty()) return noResults(pq);

        ChatResponse resp = new ChatResponse(sb.toString());
        resp.etablissements = etabs;
        resp.activites = acts;
        resp.filterHints.put("ville", capitalize(pq.city));
        return resp;
    }

    private ChatResponse filterByType(ParsedQuery pq) {
        List<Etablissement> results = allEtablissements.stream()
                .filter(e -> pq.type != null && safe(e.getType()).equalsIgnoreCase(pq.type))
                .collect(Collectors.toList());
        if (results.isEmpty()) return noResults(pq);

        ChatResponse resp = formatEtabResults(results, pq);
        resp.filterHints.put("type", capitalize(pq.type));
        resp.navigateTo = "/etablissement_tableau.fxml";
        return resp;
    }

    private ChatResponse filterByBudget(ParsedQuery pq) {
        // Filter établissements by gamme prix mapping
        List<Etablissement> etabs = allEtablissements.stream()
                .filter(e -> pq.type == null || safe(e.getType()).equalsIgnoreCase(pq.type))
                .filter(e -> pq.city == null || safe(e.getVille()).equalsIgnoreCase(pq.city))
                .filter(e -> matchesBudget(e.getGammePrix(), pq.maxBudget))
                .collect(Collectors.toList());

        // Filter activités by price
        List<Activite> acts = allActivites.stream()
                .filter(a -> pq.category == null || safe(a.getCategorie()).equalsIgnoreCase(pq.category))
                .filter(a -> a.getPrix() != null && a.getPrix().compareTo(pq.maxBudget) <= 0)
                .sorted(Comparator.comparing(Activite::getPrix))
                .collect(Collectors.toList());

        StringBuilder sb = new StringBuilder();
        sb.append(t("💰 Résultats budget ≤ **" + pq.maxBudget + " TND** :\n\n",
                "💰 Results budget ≤ **" + pq.maxBudget + " TND**:\n\n",
                "💰 نتائج الميزانية ≤ **" + pq.maxBudget + " دينار**:\n\n"));

        if (!etabs.isEmpty()) {
            sb.append(t("**Établissements** (", "**Establishments** (", "**المؤسسات** ("))
                    .append(etabs.size()).append(") :\n");
            etabs.stream().limit(5).forEach(e -> sb.append(formatEtabLine(e)));
        }
        if (!acts.isEmpty()) {
            sb.append(t("\n**Activités** (", "\n**Activities** (", "\n**الأنشطة** ("))
                    .append(acts.size()).append(") :\n");
            acts.stream().limit(5).forEach(a -> sb.append(formatActLine(a)));
        }
        if (etabs.isEmpty() && acts.isEmpty()) return noResults(pq);

        ChatResponse resp = new ChatResponse(sb.toString());
        resp.etablissements = etabs;
        resp.activites = acts;
        return resp;
    }

    // ── Activité searches ──

    private ChatResponse searchActivites(ParsedQuery pq) {
        List<Activite> results = allActivites.stream()
                .filter(a -> pq.category == null || safe(a.getCategorie()).equalsIgnoreCase(pq.category))
                .filter(a -> pq.level == null || safe(a.getNiveau()).equalsIgnoreCase(pq.level))
                .filter(a -> pq.maxBudget == null || (a.getPrix() != null && a.getPrix().compareTo(pq.maxBudget) <= 0))
                .filter(a -> "disponible".equalsIgnoreCase(safe(a.getStatut())))
                .collect(Collectors.toList());

        if (results.isEmpty()) {
            // fallback without statut filter
            results = allActivites.stream()
                    .filter(a -> pq.category == null || safe(a.getCategorie()).equalsIgnoreCase(pq.category))
                    .filter(a -> pq.level == null || safe(a.getNiveau()).equalsIgnoreCase(pq.level))
                    .filter(a -> pq.maxBudget == null || (a.getPrix() != null && a.getPrix().compareTo(pq.maxBudget) <= 0))
                    .collect(Collectors.toList());
        }

        if (results.isEmpty()) return noResults(pq);
        return formatActResults(results, pq);
    }

    private ChatResponse filterByCategory(ParsedQuery pq) {
        List<Activite> results = allActivites.stream()
                .filter(a -> safe(a.getCategorie()).equalsIgnoreCase(pq.category))
                .collect(Collectors.toList());
        if (results.isEmpty()) return noResults(pq);

        ChatResponse resp = formatActResults(results, pq);
        resp.filterHints.put("categorie", capitalize(pq.category));
        resp.navigateTo = "/activite_tableau.fxml";
        return resp;
    }

    private ChatResponse filterByLevel(ParsedQuery pq) {
        List<Activite> results = allActivites.stream()
                .filter(a -> pq.level != null && safe(a.getNiveau()).equalsIgnoreCase(pq.level))
                .filter(a -> pq.category == null || safe(a.getCategorie()).equalsIgnoreCase(pq.category))
                .collect(Collectors.toList());
        if (results.isEmpty()) return noResults(pq);

        ChatResponse resp = formatActResults(results, pq);
        resp.filterHints.put("niveau", capitalize(pq.level));
        return resp;
    }

    // ── Recommend ──

    private ChatResponse recommend(ParsedQuery pq) {
        // Pick top établissements + activités based on criteria
        List<Etablissement> etabs = allEtablissements.stream()
                .filter(e -> pq.type == null || safe(e.getType()).equalsIgnoreCase(pq.type))
                .filter(e -> pq.city == null || safe(e.getVille()).equalsIgnoreCase(pq.city))
                .limit(3)
                .collect(Collectors.toList());

        List<Activite> acts = allActivites.stream()
                .filter(a -> pq.category == null || safe(a.getCategorie()).equalsIgnoreCase(pq.category))
                .filter(a -> "disponible".equalsIgnoreCase(safe(a.getStatut())))
                .sorted(Comparator.comparing(a -> a.getPlacesDispo() == null ? 0 : -a.getPlacesDispo()))
                .limit(3)
                .collect(Collectors.toList());

        StringBuilder sb = new StringBuilder();
        sb.append(t("⭐ **Mes recommandations** :\n\n",
                "⭐ **My recommendations**:\n\n",
                "⭐ **توصياتي**:\n\n"));

        if (!etabs.isEmpty()) {
            sb.append(t("🏢 **Établissements** :\n", "🏢 **Establishments**:\n", "🏢 **المؤسسات**:\n"));
            etabs.forEach(e -> sb.append(formatEtabLine(e)));
        }
        if (!acts.isEmpty()) {
            sb.append(t("\n🎯 **Activités** :\n", "\n🎯 **Activities**:\n", "\n🎯 **الأنشطة**:\n"));
            acts.forEach(a -> sb.append(formatActLine(a)));
        }
        if (etabs.isEmpty() && acts.isEmpty()) return noResults(pq);

        ChatResponse resp = new ChatResponse(sb.toString());
        resp.etablissements = etabs;
        resp.activites = acts;
        return resp;
    }

    // ── Nearby Places (Google Places) ──

    private ChatResponse nearbyPlaces(ParsedQuery pq) {
        // Ensure we have coordinates
        if (Double.isNaN(pq.refLat) || Double.isNaN(pq.refLng)) {
            return new ChatResponse(t(
                    "📍 Je n'ai pas pu déterminer votre position de référence.\n" +
                    "Précisez un établissement ou une ville, par exemple :\n" +
                    "  • _\"restaurants proches de l'hôtel X\"_\n" +
                    "  • _\"cafés autour de Tunis\"_",
                    "📍 I couldn't determine your reference location.\n" +
                    "Please specify an establishment or city, for example:\n" +
                    "  • _\"restaurants near hotel X\"_\n" +
                    "  • _\"cafes around Tunis\"_",
                    "📍 لم أتمكن من تحديد موقعك المرجعي.\n" +
                    "حدد مؤسسة أو مدينة، مثال:\n" +
                    "  • _\"مطاعم قريبة من الفندق\"_\n" +
                    "  • _\"مقاهي حول تونس\"_"
            ));
        }

        // Check if Google Places API is configured
        if (!placesService.isConfigured()) {
            // Fall back to DB-based proximity search
            return nearbyFromDatabase(pq);
        }

        // Call Google Places API
        try {
            List<PlaceResult> places = placesService.searchNearby(
                    pq.refLat, pq.refLng, 5000, pq.nearbyType);

            if (places.isEmpty()) {
                // Fall back to DB
                return nearbyFromDatabase(pq);
            }

            return formatNearbyResults(places, pq);

        } catch (Exception e) {
            System.err.println("Google Places error: " + e.getMessage());
            // Fall back to DB-based proximity
            ChatResponse fallback = nearbyFromDatabase(pq);
            String prefix = t(
                    "⚠️ _API Google Places indisponible, recherche dans notre base :_\n\n",
                    "⚠️ _Google Places API unavailable, searching our database:_\n\n",
                    "⚠️ _واجهة Google Places غير متاحة، بحث في قاعدتنا:_\n\n"
            );
            fallback.text = prefix + fallback.text;
            return fallback;
        }
    }

    /**
     * Format Google Places API results into a rich chatbot response.
     */
    private ChatResponse formatNearbyResults(List<PlaceResult> places, ParsedQuery pq) {
        StringBuilder sb = new StringBuilder();

        // Header
        String typeLabel = pq.nearbyType != null ? capitalize(pq.nearbyType) + "s" :
                t("Lieux", "Places", "أماكن");
        sb.append(t("📍 **" + typeLabel + " à proximité",
                "📍 **Nearby " + typeLabel,
                "📍 **" + typeLabel + " القريبة"));
        if (pq.refEtabName != null) {
            sb.append(t(" de " + pq.refEtabName, " from " + pq.refEtabName, " من " + pq.refEtabName));
        } else if (pq.city != null) {
            sb.append(t(" — " + capitalize(pq.city), " — " + capitalize(pq.city), " — " + capitalize(pq.city)));
        }
        sb.append("** (Google Places)\n\n");

        // Ranking explanation
        sb.append(t("_Classés par note + popularité + proximité_\n\n",
                "_Ranked by rating + popularity + proximity_\n\n",
                "_مرتبة حسب التقييم + الشعبية + القرب_\n\n"));

        int rank = 1;
        for (PlaceResult place : places) {
            String emoji = GooglePlacesService.typeEmoji(
                    place.types != null && !place.types.isEmpty() ? place.types.get(0) : null);

            sb.append(rank).append(". ").append(emoji).append(" **").append(place.name).append("**\n");
            sb.append("   ");

            // Rating stars
            sb.append(ratingStars(place.rating));
            sb.append(" ").append(String.format("%.1f", place.rating));
            sb.append(" (").append(place.userRatingsTotal).append(t(" avis", " reviews", " تقييم")).append(")");

            // Distance
            sb.append(" · 📏 ").append(String.format("%.1f", place.distanceKm)).append(" km");

            // Price level
            if (place.priceLevel != null && !place.priceLevel.isEmpty()) {
                sb.append(" · 💰").append(place.priceLevel);
            }

            // Open now
            if (place.openNow) {
                sb.append(t(" · ✅ Ouvert", " · ✅ Open", " · ✅ مفتوح"));
            }

            sb.append("\n");

            // Address
            if (place.address != null && !place.address.isEmpty()) {
                sb.append("   📫 _").append(place.address).append("_\n");
            }

            sb.append("\n");
            rank++;
        }

        // Footer tip
        sb.append(t("💡 _Résultats enrichis par Google Places API_",
                "💡 _Results enhanced by Google Places API_",
                "💡 _النتائج مدعومة من Google Places API_"));

        ChatResponse resp = new ChatResponse(sb.toString());
        resp.nearbyPlaces = places;
        return resp;
    }

    /**
     * Fallback: search nearby using our own database (distance-based sort).
     */
    private ChatResponse nearbyFromDatabase(ParsedQuery pq) {
        // Sort all establishments by distance from reference point
        List<Etablissement> nearby = allEtablissements.stream()
                .filter(e -> e.getLatitude() != null && e.getLongitude() != null)
                .filter(e -> pq.nearbyType == null || safe(e.getType()).equalsIgnoreCase(pq.nearbyType)
                        || safe(e.getType()).equalsIgnoreCase(pq.type))
                .sorted(Comparator.comparingDouble(e ->
                        GooglePlacesService.haversineKm(pq.refLat, pq.refLng,
                                e.getLatitude(), e.getLongitude())))
                .limit(8)
                .collect(Collectors.toList());

        if (nearby.isEmpty()) return noResults(pq);

        StringBuilder sb = new StringBuilder();
        String typeLabel = pq.nearbyType != null ? capitalize(pq.nearbyType) + "s" :
                t("Établissements", "Establishments", "المؤسسات");
        sb.append(t("📍 **" + typeLabel + " les plus proches",
                "📍 **Nearest " + typeLabel,
                "📍 **أقرب " + typeLabel));
        if (pq.refEtabName != null) {
            sb.append(t(" de " + pq.refEtabName, " from " + pq.refEtabName, " من " + pq.refEtabName));
        }
        sb.append("** :\n\n");

        int rank = 1;
        for (Etablissement e : nearby) {
            double dist = GooglePlacesService.haversineKm(pq.refLat, pq.refLng,
                    e.getLatitude(), e.getLongitude());
            String emoji = getTypeEmoji(e.getType());
            sb.append(rank).append(". ").append(emoji).append(" **").append(safe(e.getNom())).append("**");
            sb.append(" — 📏 ").append(String.format("%.1f", dist)).append(" km");
            if (e.getVille() != null) sb.append(" · ").append(e.getVille());
            if (e.getGammePrix() != null) sb.append(" · 💰").append(e.getGammePrix());
            sb.append("\n");
            rank++;
        }

        sb.append(t("\n💡 _Résultats de notre base de données, triés par distance_",
                "\n💡 _Results from our database, sorted by distance_",
                "\n💡 _النتائج من قاعدتنا، مرتبة حسب المسافة_"));

        ChatResponse resp = new ChatResponse(sb.toString());
        resp.etablissements = nearby;
        return resp;
    }

    /**
     * Convert a numeric rating (0-5) to star emojis.
     */
    private String ratingStars(double rating) {
        int full = (int) rating;
        boolean half = (rating - full) >= 0.3;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < full; i++) sb.append("⭐");
        if (half) sb.append("✨");
        return sb.toString();
    }

    // ── Planning ──

    private ChatResponse generatePlanning(ParsedQuery pq) {
        List<Etablissement> etabs = allEtablissements.stream()
                .filter(e -> pq.city == null || safe(e.getVille()).equalsIgnoreCase(pq.city))
                .filter(e -> pq.type == null || safe(e.getType()).equalsIgnoreCase(pq.type))
                .collect(Collectors.toList());

        List<Activite> acts = allActivites.stream()
                .filter(a -> pq.category == null || safe(a.getCategorie()).equalsIgnoreCase(pq.category))
                .filter(a -> "disponible".equalsIgnoreCase(safe(a.getStatut())))
                .collect(Collectors.toList());

        if (etabs.isEmpty() && acts.isEmpty()) return noResults(pq);

        StringBuilder sb = new StringBuilder();
        sb.append(t("📅 **Planning personnalisé", "📅 **Personalized Schedule", "📅 **برنامج مخصص"));
        if (pq.city != null) sb.append(" — ").append(capitalize(pq.city));
        sb.append("**\n\n");

        // Morning: activity
        int slot = 0;
        String[] timeSlots = {
                t("🌅 Matin (9h-12h)", "🌅 Morning (9AM-12PM)", "🌅 الصباح (9-12)"),
                t("☀️ Après-midi (14h-17h)", "☀️ Afternoon (2PM-5PM)", "☀️ بعد الظهر (14-17)"),
                t("🌙 Soirée (19h-22h)", "🌙 Evening (7PM-10PM)", "🌙 المساء (19-22)")
        };

        // Mix activities and establishments in time slots
        Collections.shuffle(acts);
        Collections.shuffle(etabs);

        for (String ts : timeSlots) {
            sb.append("**").append(ts).append("**\n");
            if (slot < acts.size()) {
                Activite a = acts.get(slot);
                sb.append("  🎯 ").append(safe(a.getNomActivite()));
                if (a.getDuree() != null) sb.append(" (").append(a.getDuree()).append(" min)");
                if (a.getPrix() != null) sb.append(" — ").append(a.getPrix()).append(" ").append(safe(a.getDevise()));
                sb.append("\n");
            }
            if (slot < etabs.size()) {
                Etablissement e = etabs.get(slot);
                String emoji = getTypeEmoji(e.getType());
                sb.append("  ").append(emoji).append(" ").append(safe(e.getNom()));
                sb.append(" (").append(safe(e.getType())).append(")");
                if (e.getAdresse() != null) sb.append(" — ").append(e.getAdresse());
                sb.append("\n");
            }
            sb.append("\n");
            slot++;
        }

        // Budget estimation
        BigDecimal totalBudget = acts.stream().limit(3)
                .filter(a -> a.getPrix() != null)
                .map(Activite::getPrix)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        sb.append(t("💰 **Budget estimé** : ", "💰 **Estimated budget**: ", "💰 **الميزانية المقدرة**: "))
                .append(totalBudget).append(" TND\n");
        sb.append(t("ℹ️ _Planning généré automatiquement. Modifiable selon vos envies !_",
                "ℹ️ _Auto-generated schedule. Customize as you wish!_",
                "ℹ️ _تم إنشاء البرنامج تلقائياً. قابل للتعديل!_"));

        ChatResponse resp = new ChatResponse(sb.toString());
        resp.etablissements = etabs.stream().limit(3).collect(Collectors.toList());
        resp.activites = acts.stream().limit(3).collect(Collectors.toList());
        return resp;
    }

    // ── Fallback ──

    private ChatResponse fallback(ParsedQuery pq) {
        // Try fuzzy search across names
        String q = pq.rawQuery.toLowerCase();
        List<Etablissement> matchedEtab = allEtablissements.stream()
                .filter(e -> safe(e.getNom()).toLowerCase().contains(q)
                        || safe(e.getDescription()).toLowerCase().contains(q))
                .limit(3).collect(Collectors.toList());
        List<Activite> matchedAct = allActivites.stream()
                .filter(a -> safe(a.getNomActivite()).toLowerCase().contains(q)
                        || safe(a.getDescription()).toLowerCase().contains(q))
                .limit(3).collect(Collectors.toList());

        if (!matchedEtab.isEmpty() || !matchedAct.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            sb.append(t("🔍 Résultats pour \"" + pq.rawQuery + "\" :\n\n",
                    "🔍 Results for \"" + pq.rawQuery + "\":\n\n",
                    "🔍 نتائج \"" + pq.rawQuery + "\":\n\n"));
            matchedEtab.forEach(e -> sb.append(formatEtabLine(e)));
            matchedAct.forEach(a -> sb.append(formatActLine(a)));

            ChatResponse resp = new ChatResponse(sb.toString());
            resp.etablissements = matchedEtab;
            resp.activites = matchedAct;
            return resp;
        }

        return new ChatResponse(t(
                "🤔 Je n'ai pas compris votre demande.\nTapez **aide** pour voir ce que je peux faire !",
                "🤔 I didn't understand your request.\nType **help** to see what I can do!",
                "🤔 لم أفهم طلبك.\nاكتب **مساعدة** لرؤية ما يمكنني فعله!"
        ));
    }

    private ChatResponse noResults(ParsedQuery pq) {
        StringBuilder sb = new StringBuilder();
        sb.append(t("😕 Aucun résultat trouvé", "😕 No results found", "😕 لا توجد نتائج"));
        if (pq.city != null) sb.append(t(" à ", " in ", " في ")).append(capitalize(pq.city));
        if (pq.type != null) sb.append(t(" pour type ", " for type ", " للنوع ")).append(pq.type);
        if (pq.category != null) sb.append(t(" en catégorie ", " in category ", " للفئة ")).append(pq.category);
        sb.append(".\n\n");
        sb.append(t("💡 Essayez une recherche plus large ou tapez **aide**.",
                "💡 Try a broader search or type **help**.",
                "💡 حاول بحثاً أوسع أو اكتب **مساعدة**."));
        return new ChatResponse(sb.toString());
    }

    // ═══════════════════════════════════════════
    //  FORMATTING HELPERS
    // ═══════════════════════════════════════════

    private ChatResponse formatEtabResults(List<Etablissement> results, ParsedQuery pq) {
        StringBuilder sb = new StringBuilder();
        String typeLabel = pq.type != null ? capitalize(pq.type) : t("Établissement", "Establishment", "مؤسسة");
        sb.append(t("🏢 **" + typeLabel + "s trouvés** (", "🏢 **" + typeLabel + "s found** (",
                "🏢 **" + typeLabel + " وُجدت** ("))
                .append(results.size()).append(") :\n\n");
        results.stream().limit(6).forEach(e -> sb.append(formatEtabLine(e)));
        if (results.size() > 6) sb.append(t("  _... et " + (results.size()-6) + " de plus_\n",
                "  _... and " + (results.size()-6) + " more_\n",
                "  _... و " + (results.size()-6) + " أكثر_\n"));

        ChatResponse resp = new ChatResponse(sb.toString());
        resp.etablissements = results;
        return resp;
    }

    private ChatResponse formatActResults(List<Activite> results, ParsedQuery pq) {
        StringBuilder sb = new StringBuilder();
        String catLabel = pq.category != null ? capitalize(pq.category) : t("Activité", "Activity", "نشاط");
        sb.append(t("🎯 **" + catLabel + "s trouvées** (", "🎯 **" + catLabel + "s found** (",
                "🎯 **" + catLabel + " وُجدت** ("))
                .append(results.size()).append(") :\n\n");
        results.stream().limit(6).forEach(a -> sb.append(formatActLine(a)));
        if (results.size() > 6) sb.append(t("  _... et " + (results.size()-6) + " de plus_\n",
                "  _... and " + (results.size()-6) + " more_\n",
                "  _... و " + (results.size()-6) + " أكثر_\n"));

        ChatResponse resp = new ChatResponse(sb.toString());
        resp.activites = results;
        return resp;
    }

    private String formatEtabLine(Etablissement e) {
        String emoji = getTypeEmoji(e.getType());
        StringBuilder sb = new StringBuilder();
        sb.append("  ").append(emoji).append(" **").append(safe(e.getNom())).append("**");
        if (e.getVille() != null && !e.getVille().isBlank()) sb.append(" — ").append(e.getVille());
        if (e.getType() != null) sb.append(" [").append(e.getType().toUpperCase()).append("]");
        if (e.getGammePrix() != null && !e.getGammePrix().isBlank()) sb.append(" 💰").append(e.getGammePrix());
        if (e.getTelephone() != null && !e.getTelephone().isBlank()) sb.append(" 📞").append(e.getTelephone());
        sb.append("\n");
        return sb.toString();
    }

    private String formatActLine(Activite a) {
        String emoji = getCategoryEmoji(a.getCategorie());
        StringBuilder sb = new StringBuilder();
        sb.append("  ").append(emoji).append(" **").append(safe(a.getNomActivite())).append("**");
        if (a.getCategorie() != null) sb.append(" [").append(a.getCategorie().toUpperCase()).append("]");
        if (a.getPrix() != null) sb.append(" — ").append(a.getPrix()).append(" ").append(safe(a.getDevise()));
        if (a.getNiveau() != null) sb.append(" (").append(a.getNiveau()).append(")");
        if (a.getPlacesDispo() != null && a.getNbPlaces() != null)
            sb.append(" 🎫").append(a.getPlacesDispo()).append("/").append(a.getNbPlaces());
        sb.append("\n");
        return sb.toString();
    }

    // ═══════════════════════════════════════════
    //  UTILITY
    // ═══════════════════════════════════════════

    private String getTypeEmoji(String type) {
        if (type == null) return "📍";
        return switch (type.toLowerCase().trim()) {
            case "hotel" -> "🏨";
            case "restaurant" -> "🍽️";
            case "cafe" -> "☕";
            case "museum" -> "🏛️";
            case "bar" -> "🍺";
            default -> "📍";
        };
    }

    private String getCategoryEmoji(String cat) {
        if (cat == null) return "🎯";
        return switch (cat.toLowerCase().trim()) {
            case "sport" -> "⚽";
            case "culture" -> "🎭";
            case "nature" -> "🌿";
            case "gastronomie" -> "🍽️";
            case "loisir" -> "🎮";
            case "aventure" -> "🏔️";
            case "bien-etre" -> "🧘";
            default -> "🎯";
        };
    }

    private boolean matchesBudget(String gammePrix, BigDecimal max) {
        if (gammePrix == null || gammePrix.isBlank()) return true;
        // €=cheap, €€=mid, €€€=expensive
        int level = gammePrix.chars().filter(c -> c == '€' || c == '$').map(c -> 1).sum();
        if (level == 0) return true;
        if (max.compareTo(new BigDecimal("30")) <= 0) return level <= 1;
        if (max.compareTo(new BigDecimal("80")) <= 0) return level <= 2;
        return true;
    }

    private boolean matches(String q, String... keywords) {
        for (String kw : keywords) if (q.contains(kw)) return true;
        return false;
    }

    private boolean containsAny(String q, String... keywords) {
        for (String kw : keywords) if (q.contains(kw)) return true;
        return false;
    }

    /** Translate helper: pick by currentLang */
    private String t(String fr, String en, String ar) {
        return switch (currentLang) {
            case FR -> fr;
            case EN -> en;
            case AR -> ar;
        };
    }

    private String safe(String s) { return s == null ? "" : s; }
    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }
}
