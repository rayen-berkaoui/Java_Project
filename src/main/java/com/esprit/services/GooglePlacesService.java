package com.esprit.services;

import com.google.gson.*;
import okhttp3.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Service that integrates with the <b>OpenStreetMap Overpass API</b> (100% free, no key needed)
 * to provide location-based place recommendations ranked by proximity and completeness.
 *
 * <p>Usage flow:
 * <ol>
 *   <li>Call {@link #searchNearby(double, double, int, String)} with a reference location</li>
 *   <li>Results are auto-scored and sorted by (completeness + proximity)</li>
 * </ol>
 */
public class GooglePlacesService {

    // ─── Configuration ───────────────────────────────────────────
    private static final String OVERPASS_URL = "https://overpass-api.de/api/interpreter";

    private static final int DEFAULT_RADIUS_METERS = 5000; // 5 km
    private static final int MAX_RESULTS = 50;

    private final OkHttpClient httpClient;

    public GooglePlacesService() {
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    /**
     * Overpass API is always available (free, no key).
     */
    public boolean isConfigured() {
        return true;
    }

    // ─── Data class for a place result ─────────────────────────

    public static class PlaceResult {
        public String name;
        public String address;
        public double rating;           // estimated from OSM tags (0-5)
        public int userRatingsTotal;    // estimated from OSM tag completeness
        public double lat;
        public double lng;
        public double distanceKm;
        public String placeId;          // OSM node/way id
        public boolean openNow;
        public String priceLevel;       // from opening_hours heuristic
        public List<String> types;
        public double score;

        // Extra OSM fields
        public String phone;
        public String website;
        public String cuisine;
        public String wheelchair;
        public String openingHours;

        @Override
        public String toString() {
            return name + " (" + String.format("%.1f", rating) + "\u2b50 \u00b7 "
                    + String.format("%.1f", distanceKm) + " km)";
        }
    }

    // ─── Main search method ────────────────────────────────────

    /**
     * Search for places near a reference location using Overpass API (OpenStreetMap).
     *
     * @param lat    Reference latitude
     * @param lng    Reference longitude
     * @param radius Search radius in meters (default 5000)
     * @param type   Place type (restaurant, cafe, hotel, museum, bar, etc.)
     *               Can be null for all amenity types.
     * @return Sorted list of place results (best first)
     */
    public List<PlaceResult> searchNearby(double lat, double lng, int radius, String type) throws IOException {
        int r = radius > 0 ? radius : DEFAULT_RADIUS_METERS;
        String query = buildOverpassQuery(lat, lng, r, type);
        System.out.println("[Overpass] searchNearby query:\n" + query);

        RequestBody body = new FormBody.Builder()
                .add("data", query)
                .build();

        Request request = new Request.Builder()
                .url(OVERPASS_URL)
                .post(body)
                .header("Accept", "application/json")
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                throw new IOException("Overpass API error: HTTP " + response.code());
            }
            String json = response.body().string();
            return parseResults(json, lat, lng);
        }
    }

    /**
     * Convenience overload with default radius.
     */
    public List<PlaceResult> searchNearby(double lat, double lng, String type) throws IOException {
        return searchNearby(lat, lng, DEFAULT_RADIUS_METERS, type);
    }

    // ─── Overpass Query Builder ─────────────────────────────────

    /**
     * Build an Overpass QL query for nearby amenities / tourism / shops.
     */
    private String buildOverpassQuery(double lat, double lng, int radius, String type) {
        String osmFilter = mapTypeToOverpassFilter(type);
        // Query nodes and ways with a name, within the radius.
        // "out center" gives us centroid for ways.
        return "[out:json][timeout:20];\n"
                + "(\n"
                + "  node" + osmFilter + "(around:" + radius + "," + lat + "," + lng + ");\n"
                + "  way" + osmFilter + "(around:" + radius + "," + lat + "," + lng + ");\n"
                + ");\n"
                + "out center " + (MAX_RESULTS * 3) + ";\n";
    }

    /**
     * Map a user-friendly type to Overpass QL tag filter.
     */
    private String mapTypeToOverpassFilter(String type) {
        if (type == null || type.isBlank()) {
            // All common amenities + tourism + leisure + shop
            return "[\"name\"][~\"^(amenity|tourism|shop|leisure)$\"~\".\"]";
        }
        return switch (type.toLowerCase().trim()) {
            case "restaurant"  -> "[\"amenity\"=\"restaurant\"][\"name\"]";
            case "hotel"       -> "[\"tourism\"=\"hotel\"][\"name\"]";
            case "cafe"        -> "[\"amenity\"=\"cafe\"][\"name\"]";
            case "museum"      -> "[\"tourism\"=\"museum\"][\"name\"]";
            case "bar"         -> "[\"amenity\"=\"bar\"][\"name\"]";
            case "park"        -> "[\"leisure\"=\"park\"][\"name\"]";
            case "spa"         -> "[\"amenity\"~\"spa|public_bath\"][\"name\"]";
            case "pharmacy"    -> "[\"amenity\"=\"pharmacy\"][\"name\"]";
            case "shopping"    -> "[\"shop\"][\"name\"]";
            case "tourist"     -> "[\"tourism\"~\"attraction|viewpoint\"][\"name\"]";
            default            -> "[\"amenity\"=\"" + type.toLowerCase().trim() + "\"][\"name\"]";
        };
    }

    /**
     * Build a broader Overpass query that fetches ALL establishment types around a point,
     * including amenity, tourism, shop, leisure, and healthcare.
     * This is used for the "discover everything nearby" scenario.
     */
    private String buildBroadOverpassQuery(double lat, double lng, int radius) {
        return "[out:json][timeout:25];\n"
                + "(\n"
                + "  node[\"amenity\"][\"name\"](around:" + radius + "," + lat + "," + lng + ");\n"
                + "  way[\"amenity\"][\"name\"](around:" + radius + "," + lat + "," + lng + ");\n"
                + "  node[\"tourism\"][\"name\"](around:" + radius + "," + lat + "," + lng + ");\n"
                + "  way[\"tourism\"][\"name\"](around:" + radius + "," + lat + "," + lng + ");\n"
                + "  node[\"shop\"][\"name\"](around:" + radius + "," + lat + "," + lng + ");\n"
                + "  way[\"shop\"][\"name\"](around:" + radius + "," + lat + "," + lng + ");\n"
                + "  node[\"leisure\"][\"name\"](around:" + radius + "," + lat + "," + lng + ");\n"
                + "  way[\"leisure\"][\"name\"](around:" + radius + "," + lat + "," + lng + ");\n"
                + "  node[\"healthcare\"][\"name\"](around:" + radius + "," + lat + "," + lng + ");\n"
                + "  way[\"healthcare\"][\"name\"](around:" + radius + "," + lat + "," + lng + ");\n"
                + ");\n"
                + "out center " + (MAX_RESULTS * 3) + ";\n";
    }

    /**
     * Search for ALL types of establishments around a point — uses a broader Overpass query
     * that covers amenity, tourism, shop, leisure, and healthcare.
     *
     * @param lat    Reference latitude
     * @param lng    Reference longitude
     * @param radius Radius in meters
     * @return Sorted list of all nearby place results
     */
    public List<PlaceResult> searchAllNearby(double lat, double lng, int radius) throws IOException {
        int r = radius > 0 ? radius : DEFAULT_RADIUS_METERS;
        String query = buildBroadOverpassQuery(lat, lng, r);
        System.out.println("[Overpass] searchAllNearby query:\n" + query);

        RequestBody body = new FormBody.Builder()
                .add("data", query)
                .build();

        Request request = new Request.Builder()
                .url(OVERPASS_URL)
                .post(body)
                .header("Accept", "application/json")
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                throw new IOException("Overpass API error: HTTP " + response.code());
            }
            String json = response.body().string();
            return parseResults(json, lat, lng);
        }
    }

    // ─── Parsing ───────────────────────────────────────────────

    private List<PlaceResult> parseResults(String json, double refLat, double refLng) {
        List<PlaceResult> results = new ArrayList<>();
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();

        JsonArray elements = root.has("elements") ? root.getAsJsonArray("elements") : new JsonArray();
        System.out.println("[Overpass] Raw elements returned: " + elements.size());

        Set<String> seenNames = new HashSet<>(); // deduplicate

        for (JsonElement el : elements) {
            JsonObject obj = el.getAsJsonObject();
            JsonObject tags = obj.has("tags") ? obj.getAsJsonObject("tags") : new JsonObject();

            // Must have a name
            if (!tags.has("name")) continue;

            String name = tags.get("name").getAsString().trim();
            if (name.isEmpty()) continue;

            // Deduplicate by name (nodes+ways can overlap)
            if (seenNames.contains(name.toLowerCase())) continue;
            seenNames.add(name.toLowerCase());

            PlaceResult pr = new PlaceResult();
            pr.name = name;

            // Coordinates: nodes have lat/lon directly, ways have center.lat/center.lon
            if (obj.has("lat") && obj.has("lon")) {
                pr.lat = obj.get("lat").getAsDouble();
                pr.lng = obj.get("lon").getAsDouble();
            } else if (obj.has("center")) {
                JsonObject center = obj.getAsJsonObject("center");
                pr.lat = center.get("lat").getAsDouble();
                pr.lng = center.get("lon").getAsDouble();
            } else {
                continue; // skip entries without coordinates
            }

            // OSM ID
            pr.placeId = obj.get("type").getAsString() + "/" + obj.get("id").getAsLong();

            // Address construction
            pr.address = buildAddress(tags);

            // Types from OSM tags
            pr.types = extractOsmTypes(tags);

            // Extra tags
            pr.phone = getTag(tags, "phone", "contact:phone");
            pr.website = getTag(tags, "website", "contact:website");
            pr.cuisine = getTag(tags, "cuisine");
            pr.wheelchair = getTag(tags, "wheelchair");
            pr.openingHours = getTag(tags, "opening_hours");

            // Price level heuristic
            pr.priceLevel = estimatePriceLevel(tags);

            // Rating: OSM doesn't have ratings, so we estimate from tag completeness
            pr.rating = estimateRating(tags);
            pr.userRatingsTotal = countTagCompleteness(tags);

            // Opening status heuristic (simplified)
            pr.openNow = pr.openingHours != null && !pr.openingHours.isBlank();

            // Distance
            pr.distanceKm = haversineKm(refLat, refLng, pr.lat, pr.lng);

            // Composite score
            pr.score = computeScore(pr);

            results.add(pr);
        }

        // Sort by composite score (descending)
        results.sort(Comparator.comparingDouble((PlaceResult p) -> p.score).reversed());

        // Limit
        if (results.size() > MAX_RESULTS) {
            results = new ArrayList<>(results.subList(0, MAX_RESULTS));
        }

        return results;
    }

    // ─── Address builder ───────────────────────────────────────

    private String buildAddress(JsonObject tags) {
        StringBuilder sb = new StringBuilder();
        appendTag(sb, tags, "addr:housenumber");
        appendTag(sb, tags, "addr:street");
        if (sb.length() > 0) sb.append(", ");
        appendTag(sb, tags, "addr:city");
        if (sb.isEmpty()) {
            // Fallback
            String desc = getTag(tags, "description", "description:fr");
            return desc != null ? desc : "";
        }
        return sb.toString().trim();
    }

    private void appendTag(StringBuilder sb, JsonObject tags, String key) {
        if (tags.has(key)) {
            if (sb.length() > 0 && !sb.toString().endsWith(", ")) sb.append(" ");
            sb.append(tags.get(key).getAsString());
        }
    }

    // ─── OSM type extraction ───────────────────────────────────

    private List<String> extractOsmTypes(JsonObject tags) {
        List<String> types = new ArrayList<>();
        if (tags.has("amenity")) types.add(tags.get("amenity").getAsString());
        if (tags.has("tourism")) types.add(tags.get("tourism").getAsString());
        if (tags.has("shop")) types.add("shop_" + tags.get("shop").getAsString());
        if (tags.has("leisure")) types.add(tags.get("leisure").getAsString());
        if (tags.has("healthcare")) types.add("healthcare_" + tags.get("healthcare").getAsString());
        return types;
    }

    // ─── Rating & completeness heuristic ───────────────────────

    /**
     * Estimate a "quality" rating based on how well-documented the place is in OSM.
     * A place with many tags (website, phone, hours, cuisine, etc.) is likely more notable.
     */
    private double estimateRating(JsonObject tags) {
        double score = 2.5; // base
        if (tags.has("website") || tags.has("contact:website")) score += 0.5;
        if (tags.has("phone") || tags.has("contact:phone")) score += 0.3;
        if (tags.has("opening_hours")) score += 0.4;
        if (tags.has("cuisine")) score += 0.3;
        if (tags.has("wheelchair") && "yes".equals(tags.get("wheelchair").getAsString())) score += 0.2;
        if (tags.has("internet_access")) score += 0.2;
        if (tags.has("stars")) {
            try { score = Math.min(5, Double.parseDouble(tags.get("stars").getAsString())); }
            catch (NumberFormatException ignored) {}
        }
        return Math.min(5.0, score);
    }

    /**
     * Count how many informative tags a place has — used as a proxy for popularity.
     */
    private int countTagCompleteness(JsonObject tags) {
        int count = 0;
        for (String key : List.of("website", "contact:website", "phone", "contact:phone",
                "opening_hours", "cuisine", "wheelchair", "internet_access",
                "addr:street", "addr:city", "description", "stars", "rooms",
                "brand", "operator", "email", "contact:email", "fax",
                "facebook", "instagram", "twitter")) {
            if (tags.has(key)) count++;
        }
        return count;
    }

    /**
     * Rough price level from OSM tags.
     */
    private String estimatePriceLevel(JsonObject tags) {
        if (tags.has("stars")) {
            try {
                int stars = Integer.parseInt(tags.get("stars").getAsString());
                if (stars >= 5) return "\u20ac\u20ac\u20ac\u20ac";
                if (stars >= 4) return "\u20ac\u20ac\u20ac";
                if (stars >= 3) return "\u20ac\u20ac";
                return "\u20ac";
            } catch (NumberFormatException ignored) {}
        }
        return null;
    }

    // ─── Scoring algorithm ─────────────────────────────────────

    /**
     * Composite score = weighted combination of:
     * <ul>
     *   <li>Estimated quality (0\u20135) \u2192 normalized to 0\u20131, weight 0.40</li>
     *   <li>Tag completeness \u2192 normalized, weight 0.35</li>
     *   <li>Proximity (inverse of distance) \u2192 normalized, weight 0.25</li>
     * </ul>
     */
    private double computeScore(PlaceResult pr) {
        double ratingScore = pr.rating / 5.0;
        double popularityScore = Math.min(1.0, pr.userRatingsTotal / 8.0);
        double proximityScore = 1.0 / (1.0 + pr.distanceKm);
        return (0.40 * ratingScore) + (0.35 * popularityScore) + (0.25 * proximityScore);
    }

    // ─── Haversine distance ────────────────────────────────────

    public static double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    // ─── Type mapping (kept for controller compatibility) ──────

    public static String normalizeGoogleType(String userType) {
        if (userType == null) return null;
        return switch (userType.toLowerCase().trim()) {
            case "restaurant", "\u0645\u0637\u0639\u0645"           -> "restaurant";
            case "hotel", "h\u00f4tel", "\u0641\u0646\u062f\u0642"  -> "hotel";
            case "cafe", "caf\u00e9", "\u0645\u0642\u0647\u0649"    -> "cafe";
            case "museum", "mus\u00e9e", "\u0645\u062a\u062d\u0641"  -> "museum";
            case "bar"                                               -> "bar";
            case "pharmacy", "pharmacie"                             -> "pharmacy";
            case "park", "parc", "\u062d\u062f\u064a\u0642\u0629"   -> "park";
            case "spa", "hammam"                                     -> "spa";
            case "shopping", "magasin", "\u062a\u0633\u0648\u0642"   -> "shopping";
            case "tourist", "tourisme", "\u0633\u064a\u0627\u062d\u0629" -> "tourist";
            default                                                  -> userType.toLowerCase().trim();
        };
    }

    public static String typeEmoji(String osmType) {
        if (osmType == null) return "\ud83d\udccd";
        return switch (osmType.toLowerCase()) {
            case "restaurant"               -> "\ud83c\udf7d\ufe0f";
            case "hotel", "hostel", "motel" -> "\ud83c\udfe8";
            case "cafe"                     -> "\u2615";
            case "museum"                   -> "\ud83c\udfdb\ufe0f";
            case "bar", "pub"               -> "\ud83c\udf7a";
            case "park"                     -> "\ud83c\udf33";
            case "spa", "public_bath"       -> "\ud83e\uddd6";
            case "pharmacy"                 -> "\ud83d\udc8a";
            case "attraction", "viewpoint"  -> "\ud83d\udcf8";
            default -> {
                if (osmType.startsWith("shop_")) yield "\ud83d\udecd\ufe0f";
                yield "\ud83d\udccd";
            }
        };
    }

    // ─── Helpers ───────────────────────────────────────────────

    private String getTag(JsonObject tags, String... keys) {
        for (String key : keys) {
            if (tags.has(key)) return tags.get(key).getAsString();
        }
        return null;
    }
}
