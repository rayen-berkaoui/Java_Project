package com.esprit.services;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * AI Chatbot Service — Smart Travel Assistant for TABAANI SmartTravel.
 * 
 * Uses Google Gemini API when a valid key is available.
 * Falls back to a rich built-in knowledge base for offline operation.
 */
public class ChatbotService {

    // ── g_resto ChatbotPanel compatibility types ───────────────
    public enum Lang { FR, EN, AR }

    public static class ChatResponse {
        public String text;
        public java.util.List<Object> etablissements = new ArrayList<>();
        public java.util.List<Object> activites = new ArrayList<>();
        public ChatResponse(String text) { this.text = text; }
    }

    private Lang currentLang = Lang.FR;

    // ── Gemini API config (multi-model fallback) ─────────────
    private static final String API_KEY;
    static {
        String key = null;
        // 1. Environment variable
        String env = System.getenv("GEMINI_API_KEY");
        if (env != null && !env.isBlank()) key = env;
        // 2. gemini.properties file
        if (key == null) {
            try (java.io.InputStream is = ChatbotService.class.getResourceAsStream("/gemini.properties")) {
                if (is != null) {
                    java.util.Properties props = new java.util.Properties();
                    props.load(is);
                    String k = props.getProperty("api.key", "").trim();
                    if (!k.isEmpty()) key = k;
                }
            } catch (Exception ignored) {}
        }
        // 3. Hardcoded fallback
        if (key == null) key = "AIzaSyDgWisI41tprIifTlyTKdoxFrkeEIq06NU";
        API_KEY = key;
    }
    private static final String[] MODELS = {
        "gemini-2.5-flash",
        "gemini-2.0-flash-lite",
        "gemini-2.0-flash",
        "gemini-1.5-flash",
        "gemini-1.5-flash-latest",
        "gemini-1.5-pro"
    };
    private static final String API_BASE =
        "https://generativelanguage.googleapis.com/v1beta/models/";

    private static final Gson gson = new Gson();
    private final List<ChatMessage> conversationHistory = new ArrayList<>();
    private boolean useOfflineMode = API_KEY == null || API_KEY.isBlank();
    private final Random random = new Random();
    private String dbContext = "";

    private static final String SYSTEM_PROMPT = """
        Tu es "Tabaani AI", un assistant IA polyvalent et intelligent pour l'application TABAANI SmartTravel.
        Tu parles en francais par defaut, mais tu peux repondre dans la langue de l'utilisateur.

        Tu peux repondre a TOUT type de question:
        - Questions sur le tourisme en Tunisie (ta specialite)
        - Questions generales de culture, science, histoire, geographie, maths, programmation, etc.
        - Aide avec des problemes, conseils, recommandations
        - Conversations informelles

        Pour les questions liees a l'application:
        1. RECOMMANDATIONS DE VOYAGE: Suggerer des destinations, hotels, restaurants, et activites
        2. PANIER & RESERVATIONS: Aider a comprendre le systeme de panier, les prix, les promotions
        3. BUDGET: Donner des conseils pour optimiser le budget voyage
        4. CODES PROMO: SMART10=-10%, TRAVEL20=-20%, VIP15=-15%, GOLD25=-25%, WELCOME5=-5%
        5. POINTS FIDELITE: 10pts/DT avec Konnect, 100pts=5%, 200pts=10%, 500pts=20%

        Regles:
        - Utilise des emojis pour rendre tes reponses engageantes
        - Sois concis (3-5 phrases) sauf si on demande des details
        - Si on te demande de faire quelque chose de nuisible ou inapproprie, refuse poliment
        """;

    // ── Built-in Knowledge Base ─────────────────────────────────
    private static final Map<String, String[]> KNOWLEDGE_BASE = new LinkedHashMap<>();

    static {
        KNOWLEDGE_BASE.put("destination|destinations|ou aller|recommand|voyage|visiter|explorer|tunisie",
            new String[]{
                "🌍 Voici mes recommandations de destinations :\n\n" +
                "🏖️ **Djerba** — Ile paradisiaque, plages magnifiques, souk traditionnel. Budget: 150-300 DT/jour\n" +
                "🏛️ **Carthage** — Sites archeologiques UNESCO, vues sur la mer. Budget: 80-150 DT/jour\n" +
                "🏜️ **Tozeur** — Oasis, Chott El Jerid, architecture unique. Budget: 100-200 DT/jour\n" +
                "🌊 **Hammamet** — Stations balneaires, thalasso, medina. Budget: 120-250 DT/jour\n\n" +
                "💡 Conseil: Utilisez le code **TRAVEL20** pour -20% sur votre premiere reservation !",

                "🗺️ Top destinations cette saison :\n\n" +
                "1. 🌴 **Sousse** — Medina, Port El Kantaoui, ambiance festive\n" +
                "2. 🏔️ **Ain Draham** — Montagne, forets, fraicheur en ete\n" +
                "3. 🐪 **Douz** — Porte du Sahara, festivals, excursions desert\n" +
                "4. 🏖️ **Tabarka** — Plongee, corail, musique jazz\n\n" +
                "🎯 Astuce: Reservez tot pour les meilleurs prix et appliquez le code **SMART10** !",

                "✈️ Destinations incontournables :\n\n" +
                "🌟 **Sidi Bou Said** — Village bleu et blanc emblematique, cafes panoramiques\n" +
                "🏖️ **Monastir** — Ribat historique, plages familiales, prix accessibles\n" +
                "🌿 **Beja** — Nature verdoyante, thermes, tranquillite\n" +
                "🎭 **El Jem** — Colisee romain, spectacles culturels\n\n" +
                "💰 N'oubliez pas vos points fidelite : 500 pts = 20% de reduction !"
            });

        KNOWLEDGE_BASE.put("hotel|hotels|hebergement|logement|dormir|chambre|sejour luxe",
            new String[]{
                "🏨 Recommandations d'hotels :\n\n" +
                "⭐⭐⭐⭐⭐ **Hotels de luxe** (200-500 DT/nuit)\n" +
                "• The Residence, Gammarth\n• Hasdrubal Thalassa, Djerba\n\n" +
                "⭐⭐⭐⭐ **Confort** (80-200 DT/nuit)\n" +
                "• Movenpick, Sousse\n• Radisson Blu, Hammamet\n\n" +
                "⭐⭐⭐ **Economique** (40-80 DT/nuit)\n" +
                "• Ibis, Tunis\n• Dar El Medina, Tunis\n\n" +
                "💡 Code promo **VIP15** = -15% sur les hotels premium !",

                "🛏️ Conseils hebergement :\n\n" +
                "📅 **Haute saison** (juin-sept) : Reservez 2-3 mois a l'avance\n" +
                "📅 **Basse saison** (nov-mars) : Prix jusqu'a -40% !\n\n" +
                "🏠 Alternatives originales :\n" +
                "• Maisons d'hotes traditionnelles (Dar)\n" +
                "• Riads a la medina\n" +
                "• Campement desert a Douz\n\n" +
                "💰 Utilisez vos points fidelite pour des reductions supplementaires !"
            });

        KNOWLEDGE_BASE.put("promo|promotion|code|coupon|reduction|remise|offre",
            new String[]{
                "🎉 Codes promo disponibles :\n\n" +
                "🏷️ **WELCOME5** → -5% (nouveaux clients)\n" +
                "🏷️ **SMART10** → -10% (valable sur tout)\n" +
                "🏷️ **VIP15** → -15% (hotels premium)\n" +
                "🏷️ **TRAVEL20** → -20% (premiere reservation)\n" +
                "🏷️ **GOLD25** → -25% (membres Gold)\n\n" +
                "💡 Appliquez le code dans votre panier avant de payer !\n" +
                "⚡ Ces codes sont cumulables avec vos points fidelite.",

                "🎁 Offres speciales du moment :\n\n" +
                "🔥 **TRAVEL20** — Le plus populaire ! -20% sur votre 1ere reservation\n" +
                "💎 **GOLD25** — Exclusif membres Gold, -25% sur tout !\n" +
                "🆕 **WELCOME5** — Parfait pour commencer, -5%\n\n" +
                "📝 Comment appliquer :\n" +
                "1. Ajoutez au panier\n" +
                "2. Cliquez sur 'Code promo'\n" +
                "3. Entrez le code → Appliquer\n" +
                "4. La reduction s'applique instantanement !"
            });

        KNOWLEDGE_BASE.put("budget|prix|cout|argent|economiser|depense|cher|pas cher|combien",
            new String[]{
                "💰 Guide budget voyage en Tunisie :\n\n" +
                "🟢 **Budget serre** (50-100 DT/jour)\n" +
                "• Auberges, street food, transport public\n\n" +
                "🟡 **Budget moyen** (150-250 DT/jour)\n" +
                "• Hotel 3★, restaurants, excursions\n\n" +
                "🔴 **Budget confort** (300-500+ DT/jour)\n" +
                "• Hotel 5★, gastronomie, activites premium\n\n" +
                "💡 Astuces :\n" +
                "• Voyagez en basse saison (-30% en moyenne)\n" +
                "• Cumulez code promo + points fidelite\n" +
                "• Reservez en avance pour les meilleurs tarifs",

                "📊 Estimation budget par destination :\n\n" +
                "🏖️ Djerba : 150-300 DT/jour (couple)\n" +
                "🏛️ Tunis : 100-200 DT/jour\n" +
                "🏜️ Sahara : 200-400 DT/jour (excursion)\n" +
                "🌊 Hammamet : 120-280 DT/jour\n\n" +
                "🎯 Pour economiser :\n" +
                "• Code **SMART10** = -10% immediat\n" +
                "• 200 points fidelite = -10% supplementaire\n" +
                "• Total possible : jusqu'a -20% cumule !"
            });

        KNOWLEDGE_BASE.put("fidelite|point|points|loyaute|recompense|fidel",
            new String[]{
                "⭐ Programme de fidelite TABAANI :\n\n" +
                "📈 **Gagner des points :**\n" +
                "• 10 points par DT depense (paiement carte)\n" +
                "• Bonus inscription : 50 points offerts\n\n" +
                "🎁 **Utiliser vos points :**\n" +
                "• 100 points → **-5%** de reduction\n" +
                "• 200 points → **-10%** de reduction\n" +
                "• 500 points → **-20%** de reduction\n\n" +
                "💡 Cumulable avec les codes promo !\n" +
                "🔄 Les points n'expirent jamais."
            });

        KNOWLEDGE_BASE.put("panier|reservation|reserver|commander|acheter|ajouter",
            new String[]{
                "🛒 Guide panier & reservation :\n\n" +
                "📝 **Etapes :**\n" +
                "1. Parcourez les destinations et etablissements\n" +
                "2. Cliquez 'Reserver' pour ajouter au panier\n" +
                "3. Choisissez dates + nombre de personnes\n" +
                "4. Appliquez un code promo (optionnel)\n" +
                "5. Validez et payez par Konnect ou en especes\n\n" +
                "\uD83D\uDCB3 Paiement securise par Konnect\n" +
                "📄 Facture PDF generee automatiquement\n" +
                "✅ Confirmation par email instantanee",

                "📋 Vos reservations :\n\n" +
                "Pour consulter vos reservations, allez dans l'onglet **Paiements**.\n\n" +
                "🔹 **En attente** — Reservation non confirmee\n" +
                "🔹 **Confirme** — Paiement recu, vous etes pret !\n" +
                "🔹 **Annule** — Reservation annulee\n\n" +
                "💡 Vous pouvez modifier ou supprimer les articles de votre panier a tout moment.\n" +
                "📥 Telechargez votre facture depuis la page reservations."
            });

        KNOWLEDGE_BASE.put("paiement|payer|carte|konnect|bancaire|transaction|facture",
            new String[]{
                "\uD83D\uDCB3 Paiement sur TABAANI :\n\n" +
                "\uD83D\uDD12 **Methodes acceptees :**\n" +
                "\u2022 Especes (paiement en boutique)\n" +
                "\u2022 Paiement en ligne via **Konnect**\n\n" +
                "\uD83D\uDCCB **Processus Konnect :**\n" +
                "1. Validez votre panier\n" +
                "2. Cliquez sur Konnect pour ouvrir le formulaire de paiement\n" +
                "3. Payez avec votre carte bancaire, wallet ou e-DINAR\n" +
                "4. Recevez votre confirmation + facture PDF\n\n" +
                "\uD83C\uDF81 Chaque paiement Konnect vous rapporte **10 points/DT** !\n" +
                "\uD83D\uDD10 Vos donnees sont 100% securisees."
            });

        KNOWLEDGE_BASE.put("restaurant|manger|cuisine|gastronomie|plat|nourriture",
            new String[]{
                "🍽️ Gastronomie tunisienne :\n\n" +
                "🥘 **Plats incontournables :**\n" +
                "• Couscous — Le classique familial\n" +
                "• Brik a l'oeuf — Entree croustillante\n" +
                "• Ojja — Plat epice aux oeufs/merguez\n" +
                "• Lablebi — Soupe de pois chiches\n\n" +
                "🏆 **Restaurants recommandes :**\n" +
                "• Dar El Jeld (Tunis) — Cuisine raffinee\n" +
                "• Le Pirate (Sidi Bou Said) — Vue mer\n" +
                "• Dar Zarrouk (Sidi Bou Said) — Panoramique\n\n" +
                "💰 Budget moyen restaurant : 25-60 DT/personne"
            });

        KNOWLEDGE_BASE.put("bonjour|salut|hello|hey|coucou|bonsoir",
            new String[]{
                "👋 Bonjour ! Je suis **Tabaani AI**, votre assistant de voyage.\n\n" +
                "Je peux vous aider avec :\n" +
                "🌍 Recommandations de destinations\n" +
                "🏨 Suggestions d'hotels\n" +
                "💰 Conseils budget\n" +
                "🎉 Codes promo disponibles\n" +
                "⭐ Programme de fidelite\n" +
                "🛒 Aide panier & reservations\n\n" +
                "Que souhaitez-vous explorer ? 😊"
            });

        KNOWLEDGE_BASE.put("merci|super|genial|parfait|excellent|cool|top",
            new String[]{
                "😊 Avec plaisir ! N'hesitez pas si vous avez d'autres questions.\n\n" +
                "🎯 Rappel rapide :\n" +
                "• Code **TRAVEL20** = -20% premiere reservation\n" +
                "• Vos points fidelite = reductions supplementaires\n\n" +
                "Bon voyage avec TABAANI ! ✈️🌟"
            });

        KNOWLEDGE_BASE.put("aide|help|comment|fonctionn|utiliser|marche",
            new String[]{
                "❓ Comment utiliser TABAANI SmartTravel :\n\n" +
                "🏠 **Accueil** — Decouvrez les destinations et etablissements\n" +
                "📍 **Lieux** — Explorez tous les lieux disponibles\n" +
                "🏨 **Etablissements** — Hotels, restaurants, activites\n" +
                "🛒 **Panier** — Gerez vos selections\n" +
                "💳 **Paiements** — Historique et factures\n" +
                "👤 **Profil** — Vos informations et preferences\n\n" +
                "💡 Astuce : Utilisez le bouton ☀/🌙 pour changer de theme !"
            });
    }

    // ── Default fallback ────────────────────────────────────────
    private static final String[] DEFAULT_RESPONSES = {
        "🤔 Je ne suis pas sur de comprendre votre demande. Voici ce que je peux faire :\n\n" +
        "🌍 Destinations — Demandez-moi des recommandations\n" +
        "🏨 Hotels — Je connais les meilleurs hebergements\n" +
        "💰 Budget — Je vous aide a planifier\n" +
        "🎉 Promos — Codes reduction disponibles\n" +
        "⭐ Fidelite — Vos points et avantages\n\n" +
        "Essayez une de ces suggestions ! 😊",

        "💬 Je suis specialise dans le voyage et les reservations. Posez-moi des questions sur :\n\n" +
        "• Les destinations en Tunisie 🇹🇳\n" +
        "• Les hotels et restaurants 🏨🍽️\n" +
        "• Votre budget et les bons plans 💰\n" +
        "• Les codes promo actifs 🎁\n" +
        "• Le programme de fidelite ⭐\n\n" +
        "Je suis la pour vous aider !"
    };

    // ── Constructor ─────────────────────────────────────────────
    public ChatbotService() {
        conversationHistory.add(new ChatMessage("user", SYSTEM_PROMPT));
        conversationHistory.add(new ChatMessage("model",
            "Compris ! Je suis Tabaani AI, votre assistant voyage. Comment puis-je vous aider ?"));
        // Load database context in background
        Thread dbThread = new Thread(this::loadDatabaseContext);
        dbThread.setDaemon(true);
        dbThread.start();
    }

    /**
     * Send a message to the AI and get a response.
     */
    public String chat(String userMessage) {
        // Strip context metadata for keyword matching
        String cleanMsg = userMessage.toLowerCase(Locale.FRENCH);
        if (cleanMsg.contains("[contexte")) {
            cleanMsg = cleanMsg.substring(0, cleanMsg.indexOf("[contexte")).trim();
        }

        // Try online API first if key is set
        if (!useOfflineMode) {
            String apiResponse = tryApiCall(userMessage);
            if (apiResponse != null) {
                return apiResponse;
            }
            // API failed — switch to offline permanently this session
            useOfflineMode = true;
            System.out.println("ChatbotService: API call failed, switching to offline mode.");
        }

        // Offline mode — match from knowledge base
        return getOfflineResponse(cleanMsg);
    }

    /**
     * Attempt to call the Gemini API with multi-model fallback.
     * Returns null on any failure (triggers offline mode).
     */
    private String tryApiCall(String userMessage) {
        try {
            conversationHistory.add(new ChatMessage("user", userMessage));

            // Build system instruction with DB context
            String systemText = SYSTEM_PROMPT;
            if (dbContext != null && !dbContext.isBlank()) {
                systemText += "\n\nDONNEES DE LA BASE TOURISTIQUE (utilise-les pour les questions sur l'app):\n" + dbContext;
            }

            JsonObject requestBody = new JsonObject();

            // System instruction
            JsonObject systemInstruction = new JsonObject();
            JsonArray sysParts = new JsonArray();
            JsonObject sysPart = new JsonObject();
            sysPart.addProperty("text", systemText);
            sysParts.add(sysPart);
            systemInstruction.add("parts", sysParts);
            requestBody.add("system_instruction", systemInstruction);

            // Conversation history
            JsonArray contents = new JsonArray();
            for (ChatMessage msg : conversationHistory) {
                JsonObject content = new JsonObject();
                content.addProperty("role", msg.role);
                JsonArray parts = new JsonArray();
                JsonObject textPart = new JsonObject();
                textPart.addProperty("text", msg.text);
                parts.add(textPart);
                content.add("parts", parts);
                contents.add(content);
            }
            requestBody.add("contents", contents);

            JsonObject genConfig = new JsonObject();
            genConfig.addProperty("temperature", 0.7);
            genConfig.addProperty("maxOutputTokens", 800);
            genConfig.addProperty("topP", 0.9);
            requestBody.add("generationConfig", genConfig);

            String response = sendRequestWithFallback(requestBody.toString());
            JsonObject responseJson = gson.fromJson(response, JsonObject.class);

            if (responseJson.has("candidates")) {
                String aiResponse = responseJson
                    .getAsJsonArray("candidates").get(0).getAsJsonObject()
                    .getAsJsonObject("content")
                    .getAsJsonArray("parts").get(0).getAsJsonObject()
                    .get("text").getAsString();

                conversationHistory.add(new ChatMessage("model", aiResponse));
                if (conversationHistory.size() > 22) {
                    conversationHistory.subList(2, 4).clear();
                }
                return aiResponse;
            }
            conversationHistory.remove(conversationHistory.size() - 1);
            return null;

        } catch (Exception e) {
            if (!conversationHistory.isEmpty()) {
                conversationHistory.remove(conversationHistory.size() - 1);
            }
            System.err.println("Chatbot API error: " + e.getMessage());
            return null;
        }
    }

    /**
     * Match user message to knowledge base entries.
     */
    private String getOfflineResponse(String message) {
        // Find best matching knowledge base entry
        String bestKey = null;
        int bestScore = 0;

        for (String keyPattern : KNOWLEDGE_BASE.keySet()) {
            String[] keywords = keyPattern.split("\\|");
            int score = 0;
            for (String keyword : keywords) {
                if (message.contains(keyword.toLowerCase())) {
                    score += keyword.length(); // Longer matches = more specific
                }
            }
            if (score > bestScore) {
                bestScore = score;
                bestKey = keyPattern;
            }
        }

        if (bestKey != null && bestScore > 0) {
            String[] responses = KNOWLEDGE_BASE.get(bestKey);
            return responses[random.nextInt(responses.length)];
        }

        // No match — return default
        return DEFAULT_RESPONSES[random.nextInt(DEFAULT_RESPONSES.length)];
    }

    public String getPriceRecommendation(String destination, String serviceType, int nbPersonnes) {
        return chat(String.format(
            "Donne-moi une estimation de prix pour %d personne(s) pour un(e) %s a %s.",
            nbPersonnes, serviceType, destination));
    }

    public String getPanierSuggestions(String items, double totalPrice) {
        return chat(String.format(
            "L'utilisateur a dans son panier: %s pour un total de %.2f DT.",
            items, totalPrice));
    }

    public String getDestinationInsights(String destination) {
        return chat("Donne-moi les informations essentielles sur " + destination);
    }

    /**
     * Generate a standalone AI review — uses a fresh API call without chatbot history.
     * This avoids the travel assistant system prompt polluting the review output.
     */
    public String generateReview(String prompt) {
        if (API_KEY == null || API_KEY.isBlank()) {
            return "⚠️ Service IA indisponible. Veuillez rediger votre avis manuellement.";
        }
        try {
            // Build a clean two-turn conversation (system-like instruction + actual request)
            // Gemini requires alternating user/model roles
            JsonObject requestBody = new JsonObject();
            JsonArray contents = new JsonArray();

            // Turn 1: user gives system instruction
            JsonObject sysMsg = new JsonObject();
            sysMsg.addProperty("role", "user");
            JsonArray sysParts = new JsonArray();
            JsonObject sysText = new JsonObject();
            sysText.addProperty("text", "Tu es un redacteur d'avis clients en francais. Tu ecris des avis authentiques, "
                + "naturels et personnels comme un vrai voyageur. Tu ne mentionnes jamais que tu es une IA. "
                + "Tu reponds UNIQUEMENT avec l'avis demande, sans introduction ni commentaire supplementaire.");
            sysParts.add(sysText);
            sysMsg.add("parts", sysParts);
            contents.add(sysMsg);

            // Turn 2: model acknowledges
            JsonObject ackMsg = new JsonObject();
            ackMsg.addProperty("role", "model");
            JsonArray ackParts = new JsonArray();
            JsonObject ackText = new JsonObject();
            ackText.addProperty("text", "Compris, je vais rediger l'avis demande.");
            ackParts.add(ackText);
            ackMsg.add("parts", ackParts);
            contents.add(ackMsg);

            // Turn 3: user gives the actual review request
            JsonObject userMsg = new JsonObject();
            userMsg.addProperty("role", "user");
            JsonArray userParts = new JsonArray();
            JsonObject userText = new JsonObject();
            userText.addProperty("text", prompt);
            userParts.add(userText);
            userMsg.add("parts", userParts);
            contents.add(userMsg);

            requestBody.add("contents", contents);

            JsonObject genConfig = new JsonObject();
            genConfig.addProperty("temperature", 0.9);
            genConfig.addProperty("maxOutputTokens", 400);
            genConfig.addProperty("topP", 0.95);
            requestBody.add("generationConfig", genConfig);

            System.out.println("[AI REVIEW] Sending request to Gemini...");
            String response = sendRequest(requestBody.toString());
            System.out.println("[AI REVIEW] Response: " + response.substring(0, Math.min(response.length(), 500)));

            JsonObject responseJson = gson.fromJson(response, JsonObject.class);

            if (responseJson.has("candidates")) {
                JsonArray candidates = responseJson.getAsJsonArray("candidates");
                if (candidates.size() > 0) {
                    JsonObject candidate = candidates.get(0).getAsJsonObject();
                    if (candidate.has("content")) {
                        return candidate.getAsJsonObject("content")
                            .getAsJsonArray("parts").get(0).getAsJsonObject()
                            .get("text").getAsString();
                    }
                    // Candidate exists but was blocked by safety filters
                    if (candidate.has("finishReason")) {
                        String reason = candidate.get("finishReason").getAsString();
                        System.err.println("[AI REVIEW] Blocked: finishReason=" + reason);
                    }
                }
            }
            // Check for API error field
            if (responseJson.has("error")) {
                String errorMsg = responseJson.getAsJsonObject("error").has("message")
                    ? responseJson.getAsJsonObject("error").get("message").getAsString()
                    : "Unknown API error";
                System.err.println("[AI REVIEW] API Error: " + errorMsg);
                // Fall through to use chat() as fallback
            }

            // Fallback: use the regular chat() method which is known to work
            System.out.println("[AI REVIEW] Standalone call failed, falling back to chat()...");
            return chat(prompt);

        } catch (Exception e) {
            System.err.println("[AI REVIEW] Exception: " + e.getMessage());
            e.printStackTrace();
            // Fallback: use the regular chat() method
            try {
                return chat(prompt);
            } catch (Exception ex) {
                return "⚠️ Erreur de connexion a l'IA: " + e.getMessage();
            }
        }
    }

    public void clearHistory() {
        conversationHistory.clear();
        conversationHistory.add(new ChatMessage("user", SYSTEM_PROMPT));
        conversationHistory.add(new ChatMessage("model",
            "Conversation reinitialise ! Comment puis-je vous aider ?"));
    }

    /**
     * Load database context so the AI can answer questions about real data.
     */
    public void loadDatabaseContext() {
        try {
            StringBuilder sb = new StringBuilder();
            com.esprit.services.categorieServices catService = new com.esprit.services.categorieServices();
            java.util.List<com.esprit.entities.categorie> categories = catService.afficher();
            sb.append("=== CATEGORIES (").append(categories.size()).append(") ===\n");
            for (com.esprit.entities.categorie c : categories) {
                sb.append("- ").append(c.getNomcategorie());
                if (c.getDescription() != null && !c.getDescription().isEmpty())
                    sb.append(": ").append(c.getDescription());
                sb.append("\n");
            }

            com.esprit.services.AdresseServices adrService = new com.esprit.services.AdresseServices();
            java.util.List<com.esprit.entities.Adresse> adresses = adrService.afficher();
            sb.append("\n=== ADRESSES (").append(adresses.size()).append(") ===\n");
            for (com.esprit.entities.Adresse a : adresses) {
                sb.append("- ").append(a.getRue()).append(", ").append(a.getVille()).append("\n");
            }

            com.esprit.services.LieuTouristiqueServices lieuService = new com.esprit.services.LieuTouristiqueServices();
            java.util.List<com.esprit.entities.LieuTouristique> lieux = lieuService.afficher();
            java.util.Map<Integer, String> catMap = new java.util.HashMap<>();
            for (com.esprit.entities.categorie c : categories) catMap.put(c.getIdcategorie(), c.getNomcategorie());
            sb.append("\n=== LIEUX TOURISTIQUES (").append(lieux.size()).append(") ===\n");
            for (com.esprit.entities.LieuTouristique l : lieux) {
                sb.append("- ").append(l.getNom())
                    .append(" | Ville: ").append(l.getVille())
                    .append(" | Prix: ").append(l.getPrix()).append(" TND")
                    .append(" | Categorie: ").append(catMap.getOrDefault(l.getId_categorie(), "N/A"));
                if (l.getDescription() != null && !l.getDescription().isEmpty())
                    sb.append(" | Desc: ").append(l.getDescription());
                sb.append("\n");
            }
            this.dbContext = sb.toString();
            System.out.println("ChatbotService: DB context loaded (" + dbContext.length() + " chars)");
        } catch (Exception e) {
            System.err.println("ChatbotService: Could not load DB context: " + e.getMessage());
        }
    }

    /**
     * Send request with multi-model fallback (tries each model on quota/rate errors).
     */
    private String sendRequestWithFallback(String jsonBody) throws IOException {
        Exception lastError = null;
        for (String model : MODELS) {
            try {
                String url = API_BASE + model + ":generateContent?key=" + API_KEY;
                return sendRequestToUrl(url, jsonBody);
            } catch (IOException e) {
                String msg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
                if (msg.contains("quota") || msg.contains("429") || msg.contains("403") || msg.contains("rate")) {
                    System.err.println("ChatbotService: Model " + model + " quota exceeded, trying next...");
                    lastError = e;
                    continue;
                }
                throw e;
            }
        }
        throw lastError != null ? new IOException(lastError.getMessage()) : new IOException("All models unavailable");
    }

    private String sendRequestToUrl(String urlStr, String jsonBody) throws IOException {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(30000);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(jsonBody.getBytes(StandardCharsets.UTF_8));
        }

        int responseCode = conn.getResponseCode();
        if (responseCode == 429 || responseCode == 403) {
            throw new IOException("quota/rate limit: " + responseCode);
        }

        InputStream stream = (responseCode >= 200 && responseCode < 300)
            ? conn.getInputStream()
            : conn.getErrorStream();

        try (BufferedReader br = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                response.append(line);
            }
            return response.toString();
        }
    }

    private String sendRequest(String jsonBody) throws IOException {
        return sendRequestWithFallback(jsonBody);
    }

    private static class ChatMessage {
        final String role;
        final String text;
        ChatMessage(String role, String text) {
            this.role = role;
            this.text = text;
        }
    }

    // ── g_resto ChatbotPanel compatibility methods ─────────────

    public void refreshData() { /* no-op — data loaded on demand */ }

    public void setLang(Lang lang) { this.currentLang = lang; }

    public ChatResponse process(String message) {
        return new ChatResponse(chat(message));
    }

    public boolean toggleAiMode() {
        useOfflineMode = !useOfflineMode;
        return !useOfflineMode;
    }

    public boolean isAiConfigured() {
        return API_KEY != null && !API_KEY.isBlank();
    }
}
