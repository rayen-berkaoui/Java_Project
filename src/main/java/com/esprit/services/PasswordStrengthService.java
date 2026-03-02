package com.esprit.services;

import me.gosimple.nbvcxz.Nbvcxz;
import me.gosimple.nbvcxz.resources.Configuration;
import me.gosimple.nbvcxz.resources.ConfigurationBuilder;
import me.gosimple.nbvcxz.scoring.Result;
import me.gosimple.nbvcxz.scoring.TimeEstimate;

/**
 * Password Strength Meter using nbvcxz (Java port of Dropbox's zxcvbn).
 *
 * Returns detailed analysis:
 * - Score (0-4): Weak / Fair / Good / Strong / Very Strong
 * - Estimated crack time (human-readable)
 * - Feedback suggestions
 */
public class PasswordStrengthService {

    private final Nbvcxz nbvcxz;

    public PasswordStrengthService() {
        // Build configuration with French-friendly dictionary
        Configuration config = new ConfigurationBuilder()
                .setMinimumEntropy(40.0)  // Minimum bits of entropy for "acceptable"
                .createConfiguration();
        nbvcxz = new Nbvcxz(config);
    }

    // =====================================================
    // ✅ ANALYZE PASSWORD STRENGTH
    // =====================================================
    public PasswordAnalysis analyze(String password) {
        if (password == null || password.isEmpty()) {
            return new PasswordAnalysis(0, 0.0, "Très faible", "Entrez un mot de passe", "#FF4444", "0 secondes");
        }

        Result result = nbvcxz.estimate(password);

        int score = getScore(result);
        double entropy = result.getEntropy();
        String crackTime = getOfflineCrackTime(result);
        String label = getLabel(score);
        String color = getColor(score);
        String feedback = getFeedback(result, score);

        return new PasswordAnalysis(score, entropy, label, feedback, color, crackTime);
    }

    // =====================================================
    // Score mapping (0-4)
    // =====================================================
    private int getScore(Result result) {
        double entropy = result.getEntropy();
        if (entropy < 20) return 0;   // Très faible
        if (entropy < 40) return 1;   // Faible
        if (entropy < 60) return 2;   // Moyen
        if (entropy < 80) return 3;   // Fort
        return 4;                      // Très fort
    }

    private String getLabel(int score) {
        return switch (score) {
            case 0 -> "Très faible";
            case 1 -> "Faible";
            case 2 -> "Moyen";
            case 3 -> "Fort";
            case 4 -> "Très fort";
            default -> "Inconnu";
        };
    }

    private String getColor(int score) {
        return switch (score) {
            case 0 -> "#FF4444";      // Red
            case 1 -> "#FF6B6B";      // Light red
            case 2 -> "#FFB74D";      // Orange
            case 3 -> "#51CF66";      // Green
            case 4 -> "#2ECC71";      // Bright green
            default -> "#888888";
        };
    }

    private String getOfflineCrackTime(Result result) {
        // Get the estimated seconds for offline attack (10^10 guesses/sec)
        String timeStr = TimeEstimate.getTimeToCrackFormatted(result, "OFFLINE_BCRYPT_14");
        return translateTime(timeStr);
    }

    private String getFeedback(Result result, int score) {
        if (score >= 4) return "Excellent ! Mot de passe très sécurisé";
        if (score >= 3) return "Bon mot de passe";
        if (score >= 2) return "Ajoutez des caractères spéciaux pour améliorer";

        // Provide specific feedback based on patterns detected
        StringBuilder sb = new StringBuilder();
        if (result.getPassword().length() < 8) {
            sb.append("Minimum 8 caractères. ");
        }
        if (result.getPassword().matches("[a-zA-Z]+")) {
            sb.append("Ajoutez des chiffres et symboles. ");
        }
        if (result.getPassword().matches("\\d+")) {
            sb.append("Ajoutez des lettres et symboles. ");
        }
        if (sb.isEmpty()) {
            sb.append("Trop prévisible. Essayez un mot de passe unique.");
        }
        return sb.toString().trim();
    }

    /**
     * Translate common English time estimates to French
     */
    private String translateTime(String time) {
        if (time == null) return "inconnu";
        time = time.toLowerCase().trim();

        time = time.replace("instant", "instantané")
                   .replace("seconds", "secondes")
                   .replace("second", "seconde")
                   .replace("minutes", "minutes")
                   .replace("minute", "minute")
                   .replace("hours", "heures")
                   .replace("hour", "heure")
                   .replace("days", "jours")
                   .replace("day", "jour")
                   .replace("months", "mois")
                   .replace("month", "mois")
                   .replace("years", "ans")
                   .replace("year", "an")
                   .replace("centuries", "siècles")
                   .replace("century", "siècle");

        return time;
    }

    // =====================================================
    // ✅ RESULT DATA CLASS
    // =====================================================
    public static class PasswordAnalysis {
        private final int score;           // 0-4
        private final double entropy;      // Bits of entropy
        private final String label;        // "Très faible" → "Très fort"
        private final String feedback;     // Improvement suggestion
        private final String color;        // Hex color for UI
        private final String crackTime;    // Human-readable crack time

        public PasswordAnalysis(int score, double entropy, String label, String feedback, String color, String crackTime) {
            this.score = score;
            this.entropy = entropy;
            this.label = label;
            this.feedback = feedback;
            this.color = color;
            this.crackTime = crackTime;
        }

        public int getScore() { return score; }
        public double getEntropy() { return entropy; }
        public String getLabel() { return label; }
        public String getFeedback() { return feedback; }
        public String getColor() { return color; }
        public String getCrackTime() { return crackTime; }

        /**
         * Get progress value for ProgressBar (0.0 - 1.0)
         */
        public double getProgress() {
            return switch (score) {
                case 0 -> 0.1;
                case 1 -> 0.3;
                case 2 -> 0.55;
                case 3 -> 0.8;
                case 4 -> 1.0;
                default -> 0.0;
            };
        }
    }
}
