package com.esprit.services;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

/**
 * Konnect Payment Service — Tunisian online payment integration by Digitact.
 *
 * Konnect API Flow:
 *   1. Init payment → get a payUrl + paymentRef
 *   2. Open payUrl in WebView → user pays via bank card, wallet, or e-DINAR
 *   3. Verify payment status via paymentRef
 *
 * Konnect sandbox test cards:
 *   Visa:       4509211111111119  Exp: 12/26  CVV: 748
 *   MasterCard: 5440212711111110  Exp: 12/26  CVV: 665
 *
 * Credentials are loaded from konnect.properties (gitignored) with fallback to env vars.
 * To set up: create konnect.properties in project root with:
 *   konnect.api_key=YOUR_API_KEY
 *   konnect.wallet_id=YOUR_WALLET_ID
 */
public class KonnectPaymentService {

    // Konnect API base URLs
    private static final String SANDBOX_BASE = "https://api.sandbox.konnect.network/api/v2";
    private static final String PRODUCTION_BASE = "https://api.konnect.network/api/v2";

    // Use sandbox by default — set KONNECT_ENV=production for live
    private static final String BASE_URL;

    // Credentials loaded from konnect.properties → env var → placeholder
    private static final String API_KEY;
    private static final String WALLET_ID;

    // Redirect URLs — used by WebView to detect success/fail
    // NOTE: successUrl/failUrl are deprecated in Konnect API v2 but still useful for WebView redirect detection
    public static final String SUCCESS_URL = "https://tabaani.tn/payment/success";
    public static final String FAIL_URL = "https://tabaani.tn/payment/fail";

    static {
        // Determine environment
        String env = System.getenv("KONNECT_ENV");
        BASE_URL = "production".equalsIgnoreCase(env) ? PRODUCTION_BASE : SANDBOX_BASE;

        // Load credentials from konnect.properties file first
        Properties props = new Properties();
        String propApiKey = null;
        String propWalletId = null;

        // Try loading from multiple locations
        String[] propPaths = {
            "konnect.properties",
            "src/main/resources/konnect.properties",
            System.getProperty("user.dir") + "/konnect.properties"
        };

        for (String path : propPaths) {
            try (InputStream is = new FileInputStream(path)) {
                props.load(is);
                propApiKey = props.getProperty("konnect.api_key");
                propWalletId = props.getProperty("konnect.wallet_id");
                if (propApiKey != null && !propApiKey.isEmpty()) {
                    System.out.println("[KONNECT] Loaded credentials from: " + path);
                    break;
                }
            } catch (IOException ignored) {
                // Try next path
            }
        }

        // Also try classpath
        if (propApiKey == null || propApiKey.isEmpty()) {
            try (InputStream is = KonnectPaymentService.class.getClassLoader().getResourceAsStream("konnect.properties")) {
                if (is != null) {
                    props.load(is);
                    propApiKey = props.getProperty("konnect.api_key");
                    propWalletId = props.getProperty("konnect.wallet_id");
                    if (propApiKey != null) {
                        System.out.println("[KONNECT] Loaded credentials from classpath");
                    }
                }
            } catch (IOException ignored) {}
        }

        // Fallback chain: properties file → env var → system property → placeholder
        API_KEY = resolveCredential(propApiKey, "KONNECT_API_KEY", "konnect.api_key", "YOUR_KONNECT_API_KEY");
        WALLET_ID = resolveCredential(propWalletId, "KONNECT_WALLET_ID", "konnect.wallet_id", "YOUR_KONNECT_WALLET_ID");

        System.out.println("[KONNECT] Environment: " + (BASE_URL.contains("sandbox") ? "SANDBOX" : "PRODUCTION"));
        System.out.println("[KONNECT] API Key loaded: " + (API_KEY.startsWith("YOUR_") ? "NOT SET" : "OK (ends ..." + API_KEY.substring(Math.max(0, API_KEY.length() - 6)) + ")"));
        System.out.println("[KONNECT] Wallet ID loaded: " + (WALLET_ID.startsWith("YOUR_") ? "NOT SET" : "OK"));
    }

    private static String resolveCredential(String fromProps, String envKey, String sysPropKey, String placeholder) {
        if (fromProps != null && !fromProps.isEmpty() && !fromProps.startsWith("YOUR_")) return fromProps;
        String envVal = System.getenv(envKey);
        if (envVal != null && !envVal.isEmpty()) return envVal;
        String sysPropVal = System.getProperty(sysPropKey);
        if (sysPropVal != null && !sysPropVal.isEmpty()) return sysPropVal;
        return placeholder;
    }

    /**
     * Result class for payment operations
     */
    public static class PaymentResult {
        private final boolean success;
        private final String message;
        private final String paymentId;   // paymentRef from Konnect
        private final String paymentLink; // payUrl from Konnect
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
     * Initiate a Konnect payment.
     *
     * POST /payments/init-payment
     * Header: x-api-key
     * Body: receiverWalletId, amount (millimes), token, type,
     *       acceptedPaymentMethods, successUrl, failUrl, theme
     *
     * @param amountInDT Amount in Tunisian Dinars
     * @param description Payment description / order ID
     * @return PaymentResult with payUrl to open in WebView
     */
    public PaymentResult initPayment(double amountInDT, String description) {
        return initPayment(amountInDT, description, null, null, null, null);
    }

    /**
     * Initiate a Konnect payment with optional customer pre-fill fields.
     * Per Konnect API docs: amount is in Millimes (1 DT = 1000 millimes).
     * Pre-filling firstName/lastName/email/phoneNumber skips the checkout form entry.
     */
    public PaymentResult initPayment(double amountInDT, String description,
                                      String firstName, String lastName,
                                      String email, String phoneNumber) {
        try {
            if (API_KEY.startsWith("YOUR_")) {
                return new PaymentResult(false,
                    "Cles Konnect non configurees. Creez konnect.properties avec votre API key et wallet ID.",
                    null, null, "error");
            }

            // Convert DT to millimes (1 DT = 1000 millimes)
            int amountInMillimes = (int) Math.round(amountInDT * 1000);

            // Build JSON request body per Konnect API docs exactly:
            // https://docs.konnect.network/docs/en/api-integration/endpoints/initiate-payment
            JsonObject body = new JsonObject();
            body.addProperty("receiverWalletId", WALLET_ID);
            body.addProperty("amount", amountInMillimes);
            body.addProperty("token", "TND");
            body.addProperty("type", "immediate");

            // Accepted payment methods (bank_card and e-DINAR available in sandbox)
            JsonArray methods = new JsonArray();
            methods.add("bank_card");
            methods.add("e-DINAR");
            body.add("acceptedPaymentMethods", methods);

            // Description / order ID
            if (description != null && !description.isEmpty()) {
                body.addProperty("orderId", sanitizeOrderId(description));
                body.addProperty("description", description);
            }

            // Pre-fill customer contact info (skips manual entry on Konnect checkout form)
            if (firstName != null && !firstName.isEmpty()) body.addProperty("firstName", firstName);
            if (lastName != null && !lastName.isEmpty()) body.addProperty("lastName", lastName);
            if (email != null && !email.isEmpty()) body.addProperty("email", email);
            if (phoneNumber != null && !phoneNumber.isEmpty()) body.addProperty("phoneNumber", phoneNumber);

            // successUrl/failUrl are deprecated but needed for WebView redirect detection
            body.addProperty("successUrl", SUCCESS_URL);
            body.addProperty("failUrl", FAIL_URL);

            // Payment session lifespan in minutes
            body.addProperty("lifespan", 20);

            // Enable Konnect checkout form (true = Konnect shows its own form)
            body.addProperty("checkoutForm", true);

            // Add payment fees to payer amount
            body.addProperty("addPaymentFeesToAmount", true);

            // Theme: dark to match our app
            body.addProperty("theme", "dark");

            System.out.println("[KONNECT] Initiating payment for " + amountInDT + " DT (" + amountInMillimes + " millimes)");
            System.out.println("[KONNECT] Request body: " + body);

            // Make HTTP POST request
            String url = BASE_URL + "/payments/init-payment";
            HttpURLConnection conn = (HttpURLConnection) new URI(url).toURL().openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("x-api-key", API_KEY);
            conn.setDoOutput(true);
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(15000);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.toString().getBytes(StandardCharsets.UTF_8));
            }

            int responseCode = conn.getResponseCode();
            String responseBody = readResponse(conn);

            System.out.println("[KONNECT] Response " + responseCode + ": " + responseBody);

            if (responseCode == 200) {
                JsonObject json = JsonParser.parseString(responseBody).getAsJsonObject();

                String payUrl = json.has("payUrl") ? json.get("payUrl").getAsString() : null;
                String paymentRef = json.has("paymentRef") ? json.get("paymentRef").getAsString() : null;

                if (payUrl != null && paymentRef != null) {
                    return new PaymentResult(true,
                        "Lien de paiement genere avec succes !",
                        paymentRef, payUrl, "pending");
                } else {
                    return new PaymentResult(false,
                        "Reponse Konnect incomplete. Reessayez.",
                        paymentRef, null, "error");
                }
            } else {
                return new PaymentResult(false,
                    "Erreur Konnect (code " + responseCode + "): " + responseBody,
                    null, null, "error");
            }

        } catch (Exception e) {
            System.err.println("[KONNECT] Error: " + e.getMessage());
            e.printStackTrace();
            return new PaymentResult(false,
                "Erreur de connexion Konnect: " + e.getMessage(),
                null, null, "error");
        }
    }

    /**
     * Verify a Konnect payment status.
     *
     * GET /payments/{paymentRef}
     * Header: x-api-key
     *
     * @param paymentRef The payment reference returned from initPayment
     * @return PaymentResult with current status
     */
    public PaymentResult verifyPayment(String paymentRef) {
        try {
            String url = BASE_URL + "/payments/" + paymentRef;
            HttpURLConnection conn = (HttpURLConnection) new URI(url).toURL().openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("x-api-key", API_KEY);
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);

            int responseCode = conn.getResponseCode();
            String responseBody = readResponse(conn);

            System.out.println("[KONNECT VERIFY] Response " + responseCode + ": " + responseBody);

            if (responseCode == 200) {
                JsonObject json = JsonParser.parseString(responseBody).getAsJsonObject();

                // Konnect returns payment.status field
                String status = "unknown";
                if (json.has("payment") && json.getAsJsonObject("payment").has("status")) {
                    status = json.getAsJsonObject("payment").get("status").getAsString();
                } else if (json.has("status")) {
                    status = json.get("status").getAsString();
                }

                if ("completed".equalsIgnoreCase(status)) {
                    return new PaymentResult(true,
                        "Paiement Konnect confirme !",
                        paymentRef, null, "completed");
                } else {
                    return new PaymentResult(false,
                        "Paiement en attente. Statut: " + status,
                        paymentRef, null, status.toLowerCase());
                }
            } else {
                return new PaymentResult(false,
                    "Erreur de verification (code " + responseCode + ")",
                    paymentRef, null, "error");
            }
        } catch (Exception e) {
            System.err.println("[KONNECT VERIFY] Error: " + e.getMessage());
            return new PaymentResult(false,
                "Erreur de verification: " + e.getMessage(),
                paymentRef, null, "error");
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

    private String sanitizeOrderId(String input) {
        if (input == null || input.isEmpty()) return "TABAANI_" + System.currentTimeMillis();
        return input.replaceAll("[^a-zA-Z0-9_-]", "_").substring(0, Math.min(input.length(), 100));
    }
}
