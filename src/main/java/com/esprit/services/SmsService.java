package com.esprit.services;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Properties;

/**
 * SMS Service — Send SMS notifications via Twilio API.
 * 
 * Setup:
 *   1. Create free Twilio account at https://www.twilio.com
 *   2. Get your Account SID, Auth Token, and a Twilio phone number
 *   3. Create a file named "sms.properties" in the project root with:
 *        TWILIO_ACCOUNT_SID=ACxxxxxxxx
 *        TWILIO_AUTH_TOKEN=xxxxxxxx
 *        TWILIO_PHONE_NUMBER=+1xxxxxxxxxx
 *      This file is git-ignored so your secrets stay local.
 *   
 * Free trial: Can send to verified numbers only. 
 * For testing without Twilio: set SMS_TEST_MODE=true env variable to log SMS instead of sending.
 */
public class SmsService {

    // Load credentials: sms.properties file → env var → system property → placeholder
    private static final Properties SMS_PROPS = loadSmsProperties();

    private static Properties loadSmsProperties() {
        Properties props = new Properties();
        // Try loading from sms.properties in project root / working directory
        File f = new File("sms.properties");
        if (f.exists()) {
            try (FileInputStream fis = new FileInputStream(f)) {
                props.load(fis);
                System.out.println("📱 SMS credentials loaded from sms.properties");
            } catch (IOException e) {
                System.err.println("⚠️ Could not read sms.properties: " + e.getMessage());
            }
        }
        return props;
    }

    private static String resolve(String key, String defaultVal) {
        // Priority: sms.properties → env var → system property → default
        String val = SMS_PROPS.getProperty(key);
        if (val != null && !val.isBlank()) return val.trim();
        val = System.getenv(key);
        if (val != null && !val.isBlank()) return val.trim();
        val = System.getProperty(key);
        if (val != null && !val.isBlank()) return val.trim();
        return defaultVal;
    }

    private static final String TWILIO_ACCOUNT_SID = resolve("TWILIO_ACCOUNT_SID", "YOUR_TWILIO_ACCOUNT_SID");
    private static final String TWILIO_AUTH_TOKEN   = resolve("TWILIO_AUTH_TOKEN",   "YOUR_TWILIO_AUTH_TOKEN");
    private static final String TWILIO_PHONE_NUMBER = resolve("TWILIO_PHONE_NUMBER", "+15005550006");

    // Test mode: log SMS instead of sending (auto-enabled when no real credentials)
    private static final boolean TEST_MODE = "true".equalsIgnoreCase(System.getenv("SMS_TEST_MODE"))
        || TWILIO_ACCOUNT_SID.startsWith("YOUR_");

    private static final String TWILIO_API_URL = "https://api.twilio.com/2010-04-01/Accounts/%s/Messages.json";

    /**
     * Result class for SMS operations
     */
    public static class SmsResult {
        private final boolean success;
        private final String message;
        private final String sid;

        public SmsResult(boolean success, String message, String sid) {
            this.success = success;
            this.message = message;
            this.sid = sid;
        }

        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public String getSid() { return sid; }
    }

    /**
     * Send an SMS message.
     * 
     * @param toPhoneNumber Recipient phone number (with country code, e.g., +216XXXXXXXX)
     * @param messageBody The message text to send
     * @return SmsResult with success status
     */
    public SmsResult sendSms(String toPhoneNumber, String messageBody) {
        // Validate phone number
        if (toPhoneNumber == null || toPhoneNumber.trim().isEmpty()) {
            return new SmsResult(false, "Numero de telephone manquant.", null);
        }

        // Normalize phone number (add +216 for Tunisian numbers if missing)
        String normalizedPhone = normalizePhoneNumber(toPhoneNumber);

        if (TEST_MODE) {
            System.out.println("[SMS TEST MODE] Would send to: " + normalizedPhone);
            System.out.println("[SMS TEST MODE] Message: " + messageBody);
            return new SmsResult(true, "SMS envoye (mode test). Destinataire: " + normalizedPhone, "TEST_SID_" + System.currentTimeMillis());
        }

        try {
            String url = String.format(TWILIO_API_URL, TWILIO_ACCOUNT_SID);
            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(15000);

            // Basic Auth: Base64(AccountSID:AuthToken)
            String auth = Base64.getEncoder().encodeToString(
                (TWILIO_ACCOUNT_SID + ":" + TWILIO_AUTH_TOKEN).getBytes(StandardCharsets.UTF_8));
            conn.setRequestProperty("Authorization", "Basic " + auth);
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

            // Form-encoded body
            String body = "To=" + java.net.URLEncoder.encode(normalizedPhone, "UTF-8")
                + "&From=" + java.net.URLEncoder.encode(TWILIO_PHONE_NUMBER, "UTF-8")
                + "&Body=" + java.net.URLEncoder.encode(messageBody, "UTF-8");

            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.getBytes(StandardCharsets.UTF_8));
            }

            int responseCode = conn.getResponseCode();
            String responseBody = readResponse(conn);

            System.out.println("[SMS] Twilio response " + responseCode + ": " + responseBody);

            if (responseCode == 201 || responseCode == 200) {
                JsonObject json = JsonParser.parseString(responseBody).getAsJsonObject();
                String sid = json.has("sid") ? json.get("sid").getAsString() : null;
                return new SmsResult(true, "SMS envoye avec succes!", sid);
            } else {
                JsonObject json = JsonParser.parseString(responseBody).getAsJsonObject();
                String errMsg = json.has("message") ? json.get("message").getAsString() : "Erreur Twilio";
                return new SmsResult(false, "Echec envoi SMS: " + errMsg, null);
            }

        } catch (Exception e) {
            System.err.println("[SMS] Error: " + e.getMessage());
            return new SmsResult(false, "Erreur de connexion SMS: " + e.getMessage(), null);
        }
    }

    /**
     * Send a payment confirmation SMS.
     */
    public SmsResult sendPaymentConfirmation(String phoneNumber, String customerName, 
            String serviceName, String confirmationCode, double amount) {
        String msg = "TABAANI SmartTravel\n"
            + "Bonjour " + customerName + "!\n"
            + "Paiement confirme: " + String.format("%.2f DT", amount) + "\n"
            + "Service: " + serviceName + "\n"
            + "Code: " + confirmationCode + "\n"
            + "Merci de votre confiance!";
        return sendSms(phoneNumber, msg);
    }

    /**
     * Send a cash reservation SMS — "reserved, awaiting admin approval"
     */
    public SmsResult sendCashReservationSms(String phoneNumber, String customerName,
            String serviceName, String confirmationCode, double amount) {
        String msg = "TABAANI SmartTravel\n"
            + "Bonjour " + customerName + "!\n"
            + "Reservation enregistree: " + String.format("%.2f DT", amount) + "\n"
            + "Service: " + serviceName + "\n"
            + "Code: " + confirmationCode + "\n"
            + "En attente d'approbation admin.\n"
            + "Vous serez notifie une fois approuvee.";
        return sendSms(phoneNumber, msg);
    }

    /**
     * Send an OTP code via SMS.
     */
    public SmsResult sendOTP(String phoneNumber, String otpCode) {
        String msg = "TABAANI - Votre code de verification est: " + otpCode 
            + "\nCe code expire dans 60 secondes. Ne le partagez avec personne.";
        return sendSms(phoneNumber, msg);
    }

    /**
     * Send a reservation reminder SMS.
     */
    public SmsResult sendReservationReminder(String phoneNumber, String customerName,
            String serviceName, String date) {
        String msg = "TABAANI - Rappel!\n"
            + "Bonjour " + customerName + ", votre reservation chez "
            + serviceName + " est prevue le " + date + ".\n"
            + "Bon voyage!";
        return sendSms(phoneNumber, msg);
    }

    /**
     * Normalize Tunisian phone number format.
     */
    private String normalizePhoneNumber(String phone) {
        String cleaned = phone.trim().replaceAll("[\\s\\-\\(\\)]", "");
        
        // Already has country code
        if (cleaned.startsWith("+")) return cleaned;
        
        // Tunisian number without country code (8 digits starting with 2,3,4,5,7,9)
        if (cleaned.length() == 8 && cleaned.matches("[234579]\\d{7}")) {
            return "+216" + cleaned;
        }
        
        // Has 216 prefix but no +
        if (cleaned.startsWith("216") && cleaned.length() == 11) {
            return "+" + cleaned;
        }
        
        // Starts with 00216
        if (cleaned.startsWith("00216")) {
            return "+" + cleaned.substring(2);
        }
        
        return cleaned;
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

    /**
     * Check if SMS service is configured (not in test mode with placeholder credentials).
     */
    public boolean isConfigured() {
        return !TWILIO_ACCOUNT_SID.startsWith("YOUR_") && !TWILIO_AUTH_TOKEN.startsWith("YOUR_");
    }
}
