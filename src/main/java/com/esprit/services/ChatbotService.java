package com.esprit.services;

import com.esprit.entities.Activite;
import com.esprit.entities.Etablissement;

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

    // ─── Cached data (refreshed per session) ───
    private List<Etablissement> allEtablissements = Collections.emptyList();
    private List<Activite> allActivites = Collections.emptyList();

    // ─── Language ───
    public enum Lang { FR, EN, AR }
    private Lang currentLang = Lang.FR;

    public void setLang(Lang lang) { this.currentLang = lang; }
    public Lang getLang() { return currentLang; }

    // ─── Intent ───
    public enum Intent {
        SEARCH_ETABLISSEMENT, SEARCH_ACTIVITE,
        FILTER_BY_CITY, FILTER_BY_TYPE, FILTER_BY_CATEGORY,
        FILTER_BY_BUDGET, FILTER_BY_LEVEL,
        RECOMMEND, PLANNING,
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
    }

    /** A chatbot response */
    public static class ChatResponse {
        public String text;
        public List<Etablissement> etablissements = new ArrayList<>();
        public List<Activite> activites = new ArrayList<>();
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
    }

    /** Main entry: process a user message and return a response */
    public ChatResponse process(String userMessage) {
        if (allEtablissements.isEmpty() && allActivites.isEmpty()) refreshData();

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
