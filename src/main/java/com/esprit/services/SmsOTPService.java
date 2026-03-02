package com.esprit.services;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;

/**
 * SMS OTP Service via Twilio API.
 *
 * ⚠️ CONFIGURATION REQUIRED:
 * Replace ACCOUNT_SID, AUTH_TOKEN, and FROM_PHONE with your Twilio credentials.
 * Get them from: https://console.twilio.com/
 *
 * Free trial includes a Twilio phone number and limited SMS.
 */
public class SmsOTPService {

    // ══════════════════════════════════════════
    // ⚠️ REPLACE WITH YOUR TWILIO CREDENTIALS
    // ══════════════════════════════════════════
    private static final String ACCOUNT_SID = "TWILIO_ACCOUNT_SID";
    private static final String AUTH_TOKEN = "TWILIO_AUTH_TOKEN";
    private static final String FROM_PHONE = "+13526965556"; // Your Twilio phone number

    private static boolean initialized = false;

    public SmsOTPService() {
        if (!initialized) {
            try {
                Twilio.init(ACCOUNT_SID, AUTH_TOKEN);
                initialized = true;
                System.out.println("✅ Twilio SDK initialized");
            } catch (Exception e) {
                System.out.println("⚠️ Twilio init failed (check credentials): " + e.getMessage());
            }
        }
    }

    // =====================================================
    // ✅ SEND SMS OTP
    // =====================================================
    public boolean sendSmsOTP(String toPhoneNumber, String otpCode) {
        try {
            // Format phone number: ensure it starts with +
            String formattedPhone = formatPhoneNumber(toPhoneNumber);

            String messageBody = String.format(
                "🔐 Tabaani - Code de vérification\n\n" +
                "Votre code OTP est : %s\n\n" +
                "⏱ Ce code expire dans 60 secondes.\n" +
                "Ne partagez ce code avec personne.",
                otpCode
            );

            Message message = Message.creator(
                new PhoneNumber(formattedPhone),
                new PhoneNumber(FROM_PHONE),
                messageBody
            ).create();

            System.out.println("✅ SMS OTP sent to " + formattedPhone + " | SID: " + message.getSid());
            return true;

        } catch (Exception e) {
            System.out.println("❌ Failed to send SMS: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    // =====================================================
    // ✅ SEND CUSTOM SMS MESSAGE
    // =====================================================
    public boolean sendSms(String toPhoneNumber, String body) {
        try {
            String formattedPhone = formatPhoneNumber(toPhoneNumber);

            Message message = Message.creator(
                new PhoneNumber(formattedPhone),
                new PhoneNumber(FROM_PHONE),
                body
            ).create();

            System.out.println("✅ SMS sent to " + formattedPhone + " | SID: " + message.getSid());
            return true;

        } catch (Exception e) {
            System.out.println("❌ Failed to send SMS: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    // =====================================================
    // ✅ FORMAT PHONE NUMBER (Tunisia +216 default)
    // =====================================================
    private String formatPhoneNumber(String phone) {
        if (phone == null || phone.isEmpty()) return phone;

        // Remove spaces, dashes, dots
        phone = phone.replaceAll("[\\s\\-\\.]", "");

        // If already starts with +, return as-is
        if (phone.startsWith("+")) return phone;

        // If starts with 00, replace with +
        if (phone.startsWith("00")) return "+" + phone.substring(2);

        // For Tunisia: if 8 digits, prepend +216
        if (phone.length() == 8 && phone.matches("\\d+")) {
            return "+216" + phone;
        }

        // Otherwise just prepend +
        return "+" + phone;
    }

    // =====================================================
    // ✅ CHECK IF TWILIO IS CONFIGURED
    // =====================================================
    public boolean isConfigured() {
        return ACCOUNT_SID != null && !ACCOUNT_SID.isEmpty()
            && !ACCOUNT_SID.startsWith("YOUR_")
            && AUTH_TOKEN != null && !AUTH_TOKEN.isEmpty()
            && !AUTH_TOKEN.startsWith("YOUR_")
            && FROM_PHONE != null && !FROM_PHONE.isEmpty()
            && !FROM_PHONE.equals("+1XXXXXXXXXX")
            && initialized;
    }
}
