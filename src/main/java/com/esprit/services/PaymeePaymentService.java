package com.esprit.services;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;

/**
 * Paymee Payment Service — Tunisian online payment integration.
 *
 * Paymee API Flow:
 *   1. Create payment → get a payment token + payment URL
 *   2. Open payment URL in WebView → user pays via bank card or e-DINAR
 *   3. Check payment status via payment token
 *
 * Sandbox credentials:
 *   Account: 4313
 *   API Token: 9c412b9908dddb65bfad874adfb14baa02371081
 */
public class PaymeePaymentService {

    private static final String SANDBOX_BASE = "https://sandbox.paymee.tn/api/v2";
    private static final String PRODUCTION_BASE = "https://app.paymee.tn/api/v2";

    private static final String BASE_URL;
    private static final String API_TOKEN;

    public static final String SUCCESS_URL = "https://tabaani.tn/payment/success";
    public static final String FAIL_URL = "https://tabaani.tn/payment/fail";

    static {
        String env = System.getenv("PAYMEE_ENV");
        BASE_URL = "production".equalsIgnoreCase(env) ? PRODUCTION_BASE : SANDBOX_BASE;
        
        String token = System.getenv("PAYMEE_API_TOKEN");
        if (token == null || token.isEmpty()) {
            token = "9c412b9908dddb65bfad874adfb14baa02371081";
        }
        API_TOKEN = token;

        System.out.println("[PAYMEE] Environment: " + (BASE_URL.contains("sandbox") ? "SANDBOX" : "PRODUCTION"));
        System.out.println("[PAYMEE] API Token loaded: OK");
    }

    public static class PaymentResult {
        private final boolean success;
        private final String message;
        private final String paymentId;   // payment token from Paymee
        private final String paymentLink; // payment URL
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

    public PaymentResult initPayment(double amountInDT, String description) {
        return initPayment(amountInDT, description, null, null, null, null);
    }

    /**
     * Create a Paymee payment.
     *
     * POST /payments/create
     * Header: Authorization: Token {API_TOKEN}
     * Body: vendor (account number), amount, note, first_name, last_name, email, phone,
     *       return_url, cancel_url, order_id
     */
    public PaymentResult initPayment(double amountInDT, String description,
                                      String firstName, String lastName,
                                      String email, String phoneNumber) {
        try {
            // Build JSON request body
            JsonObject body = new JsonObject();
            body.addProperty("vendor", 4313);
            body.addProperty("amount", amountInDT);
            body.addProperty("note", description != null ? description : "Reservation TABAANI");

            if (firstName != null && !firstName.isEmpty()) body.addProperty("first_name", firstName);
            if (lastName != null && !lastName.isEmpty()) body.addProperty("last_name", lastName);
            if (email != null && !email.isEmpty()) body.addProperty("email", email);
            if (phoneNumber != null && !phoneNumber.isEmpty()) body.addProperty("phone", phoneNumber);

            body.addProperty("return_url", SUCCESS_URL);
            body.addProperty("cancel_url", FAIL_URL);
            body.addProperty("webhook_url", SUCCESS_URL);

            if (description != null && !description.isEmpty()) {
                body.addProperty("order_id", sanitizeOrderId(description));
            }

            System.out.println("[PAYMEE] Initiating payment for " + amountInDT + " DT");

            String url = BASE_URL + "/payments/create";
            HttpURLConnection conn = (HttpURLConnection) new URI(url).toURL().openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Authorization", "Token " + API_TOKEN);
            conn.setDoOutput(true);
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(15000);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.toString().getBytes(StandardCharsets.UTF_8));
            }

            int responseCode = conn.getResponseCode();
            String responseBody = readResponse(conn);

            System.out.println("[PAYMEE] Response " + responseCode + ": " + responseBody);

            if (responseCode == 200) {
                JsonObject json = JsonParser.parseString(responseBody).getAsJsonObject();
                boolean status = json.has("status") && json.get("status").getAsBoolean();

                if (status && json.has("data")) {
                    JsonObject data = json.getAsJsonObject("data");
                    String paymentToken = data.has("token") ? data.get("token").getAsString() : null;

                    if (paymentToken != null) {
                        // Payment URL is gateway + token
                        String paymentUrl = (BASE_URL.contains("sandbox")
                            ? "https://sandbox.paymee.tn/gateway/"
                            : "https://app.paymee.tn/gateway/") + paymentToken;

                        return new PaymentResult(true,
                            "Lien de paiement genere avec succes !",
                            paymentToken, paymentUrl, "pending");
                    }
                }

                String msg = json.has("message") ? json.get("message").getAsString() : "Reponse incomplete";
                return new PaymentResult(false, "Erreur Paymee: " + msg, null, null, "error");
            } else {
                return new PaymentResult(false,
                    "Erreur Paymee (code " + responseCode + "): " + responseBody,
                    null, null, "error");
            }

        } catch (Exception e) {
            System.err.println("[PAYMEE] Error: " + e.getMessage());
            e.printStackTrace();
            return new PaymentResult(false,
                "Erreur de connexion Paymee: " + e.getMessage(),
                null, null, "error");
        }
    }

    /**
     * Check a Paymee payment status.
     *
     * GET /payments/{token}/check
     * Header: Authorization: Token {API_TOKEN}
     */
    public PaymentResult verifyPayment(String paymentToken) {
        try {
            String url = BASE_URL + "/payments/" + paymentToken + "/check";
            HttpURLConnection conn = (HttpURLConnection) new URI(url).toURL().openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Authorization", "Token " + API_TOKEN);
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);

            int responseCode = conn.getResponseCode();
            String responseBody = readResponse(conn);

            System.out.println("[PAYMEE VERIFY] Response " + responseCode + ": " + responseBody);

            if (responseCode == 200) {
                JsonObject json = JsonParser.parseString(responseBody).getAsJsonObject();

                if (json.has("data")) {
                    JsonObject data = json.getAsJsonObject("data");
                    boolean paymentStatus = data.has("payment_status") && data.get("payment_status").getAsBoolean();

                    if (paymentStatus) {
                        return new PaymentResult(true,
                            "Paiement Paymee confirme !",
                            paymentToken, null, "completed");
                    } else {
                        return new PaymentResult(false,
                            "Paiement en attente.",
                            paymentToken, null, "pending");
                    }
                }

                return new PaymentResult(false,
                    "Reponse de verification incomplete.",
                    paymentToken, null, "error");
            } else {
                return new PaymentResult(false,
                    "Erreur de verification (code " + responseCode + ")",
                    paymentToken, null, "error");
            }
        } catch (Exception e) {
            System.err.println("[PAYMEE VERIFY] Error: " + e.getMessage());
            return new PaymentResult(false,
                "Erreur de verification: " + e.getMessage(),
                paymentToken, null, "error");
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
