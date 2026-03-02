package com.esprit.services;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * Flouci Payment Service — Tunisian online payment integration.
 * 
 * Flouci API Flow:
 *   1. Generate payment → get a payment link + payment_id
 *   2. Open link in WebView → user pays via Flouci app
 *   3. Verify payment status via payment_id
 * 
 * Test credentials (Flouci sandbox):
 *   app_token: d7443b0b-a004-4220-8c68-e63b1582baaa
 *   app_secret: 22189b3c-b6cf-4bca-bfa7-3b9470bc38f0
 */
public class FlouciPaymentService {

    // Flouci API endpoints
    private static final String GENERATE_URL = "https://developers.flouci.com/api/generate_payment";
    private static final String VERIFY_URL = "https://developers.flouci.com/api/verify_payment/";

    // Flouci credentials — set via environment variable or use test keys
    private static final String APP_TOKEN = System.getenv("FLOUCI_APP_TOKEN") != null
        ? System.getenv("FLOUCI_APP_TOKEN")
        : "d7443b0b-a004-4220-8c68-e63b1582baaa";

    private static final String APP_SECRET = System.getenv("FLOUCI_APP_SECRET") != null
        ? System.getenv("FLOUCI_APP_SECRET")
        : "22189b3c-b6cf-4bca-bfa7-3b9470bc38f0";

    // Redirect URLs — Flouci redirects here after success/fail
    public static final String SUCCESS_URL = "https://tabaani.tn/payment/success";
    public static final String FAIL_URL = "https://tabaani.tn/payment/fail";

    /**
     * Result class for payment operations
     */
    public static class PaymentResult {
        private final boolean success;
        private final String message;
        private final String paymentId;
        private final String paymentLink;
        private final String status;

        public PaymentResult(boolean success, String message, String paymentId, String paymentLink, String status) {
            this.success = success;
            this.message = message;
            this.paymentId = paymentId;
            this.paymentLink = paymentLink;
            this.status = status;
        }

        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public String getPaymentId() { return paymentId; }
        public String getPaymentLink() { return paymentLink; }
        public String getStatus() { return status; }
    }

    /**
     * Generate a Flouci payment link.
     * 
     * According to Flouci API spec, generate_payment endpoint requires:
     *   - app_token, app_secret, amount, success_link, fail_link in the JSON body
     *   - No auth headers required for this endpoint
     * 
     * @param amountInDT Amount in Tunisian Dinars
     * @param description Payment description / tracking ID
     * @return PaymentResult with payment link to open in WebView
     */
    public PaymentResult generatePayment(double amountInDT, String description) {
        try {
            // Convert DT to millimes (1 DT = 1000 millimes)
            int amountInMillimes = (int) Math.round(amountInDT * 1000);

            // Build JSON request body — field names per official Flouci API spec
            JsonObject body = new JsonObject();
            body.addProperty("app_token", APP_TOKEN);
            body.addProperty("app_secret", APP_SECRET);
            body.addProperty("amount", String.valueOf(amountInMillimes));
            body.addProperty("accept_card", "true");
            body.addProperty("success_link", SUCCESS_URL);
            body.addProperty("fail_link", FAIL_URL);
            body.addProperty("session_timeout_secs", 1200);
            body.addProperty("developer_tracking_id", sanitizeTrackingId(description));

            System.out.println("[FLOUCI] Generating payment for " + amountInDT + " DT (" + amountInMillimes + " millimes)");
            System.out.println("[FLOUCI] Request body: " + body);

            // Make HTTP POST request — generate_payment does NOT need auth headers
            HttpURLConnection conn = (HttpURLConnection) new URI(GENERATE_URL).toURL().openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(15000);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.toString().getBytes(StandardCharsets.UTF_8));
            }

            int responseCode = conn.getResponseCode();
            String responseBody = readResponse(conn);

            System.out.println("[FLOUCI] Response " + responseCode + ": " + responseBody);

            if (responseCode == 200) {
                JsonObject json = JsonParser.parseString(responseBody).getAsJsonObject();
                JsonObject result = json.has("result") ? json.getAsJsonObject("result") : json;

                String paymentId = result.has("payment_id") ? result.get("payment_id").getAsString() : null;
                String link = result.has("link") ? result.get("link").getAsString() : null;

                if (link != null && paymentId != null) {
                    return new PaymentResult(true,
                        "Lien de paiement genere avec succes !",
                        paymentId, link, "pending");
                } else {
                    return new PaymentResult(false,
                        "Reponse Flouci incomplete. Reessayez.",
                        paymentId, null, "error");
                }
            } else {
                return new PaymentResult(false,
                    "Erreur Flouci (code " + responseCode + "): " + responseBody,
                    null, null, "error");
            }

        } catch (Exception e) {
            System.err.println("[FLOUCI] Error: " + e.getMessage());
            e.printStackTrace();
            return new PaymentResult(false,
                "Erreur de connexion Flouci: " + e.getMessage(),
                null, null, "error");
        }
    }

    /**
     * Verify a Flouci payment status.
     * This endpoint REQUIRES auth headers: apppublic + appsecret
     * 
     * @param paymentId The payment ID returned from generatePayment
     * @return PaymentResult with current status
     */
    public PaymentResult verifyPayment(String paymentId) {
        try {
            HttpURLConnection conn = (HttpURLConnection) new URI(VERIFY_URL + paymentId).toURL().openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("apppublic", APP_TOKEN);
            conn.setRequestProperty("appsecret", APP_SECRET);
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);

            int responseCode = conn.getResponseCode();
            String responseBody = readResponse(conn);

            System.out.println("[FLOUCI VERIFY] Response " + responseCode + ": " + responseBody);

            if (responseCode == 200) {
                JsonObject json = JsonParser.parseString(responseBody).getAsJsonObject();
                JsonObject result = json.has("result") ? json.getAsJsonObject("result") : json;

                String status = result.has("status") ? result.get("status").getAsString() : "unknown";

                if ("SUCCESS".equalsIgnoreCase(status)) {
                    return new PaymentResult(true,
                        "Paiement Flouci reussi !",
                        paymentId, null, "success");
                } else {
                    return new PaymentResult(false,
                        "Paiement en attente ou refuse. Statut: " + status,
                        paymentId, null, status.toLowerCase());
                }
            } else {
                return new PaymentResult(false,
                    "Erreur de verification (code " + responseCode + ")",
                    paymentId, null, "error");
            }
        } catch (Exception e) {
            System.err.println("[FLOUCI VERIFY] Error: " + e.getMessage());
            return new PaymentResult(false,
                "Erreur de verification: " + e.getMessage(),
                paymentId, null, "error");
        }
    }

    private String readResponse(HttpURLConnection conn) throws IOException {
        InputStream is;
        try {
            is = conn.getInputStream();
        } catch (IOException e) {
            is = conn.getErrorStream();
        }
        if (is == null) return "";
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        }
    }

    private String sanitizeTrackingId(String input) {
        if (input == null || input.isEmpty()) return "tabaani_" + System.currentTimeMillis();
        return input.replaceAll("[^a-zA-Z0-9_-]", "_").substring(0, Math.min(input.length(), 100));
    }
}
