package com.esprit.services;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.model.PaymentMethod;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.PaymentMethodCreateParams;

/**
 * Stripe Payment Service — Real payment processing integration.
 * 
 * Uses Stripe TEST mode for development (no real charges).
 * Test cards:
 *   - 4242 4242 4242 4242 → Success
 *   - 4000 0000 0000 0002 → Declined
 *   - 4000 0025 0000 3155 → Requires 3D Secure
 */
public class StripePaymentService {

    // Stripe TEST Secret Key — set via environment variable or replace with your key
    private static final String STRIPE_SECRET_KEY = System.getenv("STRIPE_SECRET_KEY") != null
        ? System.getenv("STRIPE_SECRET_KEY")
        : "sk_test_YOUR_STRIPE_KEY_HERE";

    static {
        Stripe.apiKey = STRIPE_SECRET_KEY;
    }

    /**
     * Result class for payment operations
     */
    public static class PaymentResult {
        private final boolean success;
        private final String message;
        private final String paymentIntentId;
        private final String status;

        public PaymentResult(boolean success, String message, String paymentIntentId, String status) {
            this.success = success;
            this.message = message;
            this.paymentIntentId = paymentIntentId;
            this.status = status;
        }

        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public String getPaymentIntentId() { return paymentIntentId; }
        public String getStatus() { return status; }
    }

    /**
     * Process a card payment using Stripe.
     * 
     * @param amountInDT Amount in Tunisian Dinars
     * @param cardNumber Card number (test: 4242424242424242)
     * @param expMonth Expiration month (1-12)
     * @param expYear Expiration year (e.g., 2027)
     * @param cvc CVC code
     * @param description Payment description
     * @param customerEmail Customer's email
     * @return PaymentResult with success status and details
     */
    public PaymentResult processPayment(double amountInDT, String cardNumber, 
            int expMonth, int expYear, String cvc,
            String description, String customerEmail) {
        try {
            // Convert DT to millimes (Stripe uses smallest currency unit)
            // 1 DT = 1000 millimes
            long amountInMillimes = Math.round(amountInDT * 1000);

            // Step 1: Create a PaymentMethod with card details
            PaymentMethodCreateParams pmParams = PaymentMethodCreateParams.builder()
                .setType(PaymentMethodCreateParams.Type.CARD)
                .setCard(PaymentMethodCreateParams.CardDetails.builder()
                    .setNumber(cardNumber)
                    .setExpMonth((long) expMonth)
                    .setExpYear((long) expYear)
                    .setCvc(cvc)
                    .build())
                .build();

            PaymentMethod paymentMethod = PaymentMethod.create(pmParams);

            // Step 2: Create and confirm a PaymentIntent
            PaymentIntentCreateParams piParams = PaymentIntentCreateParams.builder()
                .setAmount(amountInMillimes)
                .setCurrency("tnd") // Tunisian Dinar
                .setPaymentMethod(paymentMethod.getId())
                .setConfirm(true)
                .setDescription(description)
                .setReceiptEmail(customerEmail)
                .setAutomaticPaymentMethods(
                    PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                        .setEnabled(true)
                        .setAllowRedirects(PaymentIntentCreateParams.AutomaticPaymentMethods.AllowRedirects.NEVER)
                        .build()
                )
                .build();

            PaymentIntent paymentIntent = PaymentIntent.create(piParams);

            String status = paymentIntent.getStatus();

            if ("succeeded".equals(status)) {
                return new PaymentResult(true, 
                    "Paiement reussi ! ID: " + paymentIntent.getId(),
                    paymentIntent.getId(), status);
            } else if ("requires_action".equals(status)) {
                return new PaymentResult(false,
                    "Verification 3D Secure requise. Veuillez reessayer avec une autre carte.",
                    paymentIntent.getId(), status);
            } else {
                return new PaymentResult(false,
                    "Statut du paiement: " + status,
                    paymentIntent.getId(), status);
            }

        } catch (StripeException e) {
            System.err.println("Stripe payment error: " + e.getMessage());
            String userMessage;
            switch (e.getCode()) {
                case "card_declined":
                    userMessage = "Carte refusee. Verifiez vos informations ou utilisez une autre carte.";
                    break;
                case "expired_card":
                    userMessage = "Carte expiree. Veuillez utiliser une carte valide.";
                    break;
                case "incorrect_cvc":
                    userMessage = "Code CVC incorrect. Verifiez le code au dos de votre carte.";
                    break;
                case "insufficient_funds":
                    userMessage = "Fonds insuffisants sur la carte.";
                    break;
                case "processing_error":
                    userMessage = "Erreur de traitement. Reessayez dans quelques instants.";
                    break;
                default:
                    userMessage = "Erreur de paiement: " + e.getMessage();
                    break;
            }
            return new PaymentResult(false, userMessage, null, "error");
        } catch (Exception e) {
            System.err.println("Payment processing error: " + e.getMessage());
            return new PaymentResult(false, 
                "Erreur inattendue lors du paiement. Verifiez votre connexion.",
                null, "error");
        }
    }

    /**
     * Validate card number using Luhn algorithm
     */
    public boolean isValidCardNumber(String number) {
        String cleaned = number.replaceAll("\\s", "");
        if (cleaned.length() < 13 || cleaned.length() > 19) return false;
        if (!cleaned.matches("\\d+")) return false;

        int sum = 0;
        boolean alternate = false;
        for (int i = cleaned.length() - 1; i >= 0; i--) {
            int n = cleaned.charAt(i) - '0';
            if (alternate) {
                n *= 2;
                if (n > 9) n -= 9;
            }
            sum += n;
            alternate = !alternate;
        }
        return sum % 10 == 0;
    }

    /**
     * Detect card brand from number
     */
    public String getCardBrand(String number) {
        String cleaned = number.replaceAll("\\s", "");
        if (cleaned.startsWith("4")) return "Visa";
        if (cleaned.matches("^5[1-5].*") || cleaned.matches("^2[2-7].*")) return "Mastercard";
        if (cleaned.matches("^3[47].*")) return "American Express";
        if (cleaned.startsWith("6011") || cleaned.startsWith("65")) return "Discover";
        return "Carte";
    }

    /**
     * Format card number with spaces (4-digit groups)
     */
    public String formatCardNumber(String number) {
        String cleaned = number.replaceAll("\\s", "");
        StringBuilder formatted = new StringBuilder();
        for (int i = 0; i < cleaned.length(); i++) {
            if (i > 0 && i % 4 == 0) formatted.append(' ');
            formatted.append(cleaned.charAt(i));
        }
        return formatted.toString();
    }
}
