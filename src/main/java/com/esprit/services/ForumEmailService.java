package com.esprit.services;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import java.io.InputStream;
import java.util.Properties;

public class ForumEmailService {

    private static final String CONFIG_PATH = "/moderation/moderation.properties";

    private final String smtpHost;
    private final int smtpPort;
    private final String senderEmail;
    private final String senderPassword;
    private final boolean enabled;

    public ForumEmailService() {
        Properties config = loadConfig();
        this.smtpHost = config.getProperty("mail.smtp.host", "smtp.gmail.com");
        this.smtpPort = Integer.parseInt(config.getProperty("mail.smtp.port", "587"));
        this.senderEmail = config.getProperty("mail.sender.email", "");
        this.senderPassword = config.getProperty("mail.sender.password", "");
        this.enabled = "true".equalsIgnoreCase(config.getProperty("mail.enabled", "true"));
    }

    private Properties loadConfig() {
        Properties props = new Properties();
        try (InputStream is = getClass().getResourceAsStream(CONFIG_PATH)) {
            if (is != null) props.load(is);
        } catch (Exception e) {
            System.err.println("[ForumEmailService] Impossible de charger la configuration: " + e.getMessage());
        }
        return props;
    }

    public boolean sendWarning1(String toEmail, String userKey, String badWord) {
        String subject = "⚠️ Alerte 1 — Utilisation de langage inapproprié";
        String body = buildWarning1Body(userKey, badWord);
        return sendEmail(toEmail, subject, body);
    }

    public boolean sendWarning2(String toEmail, String userKey, String badWord) {
        String subject = "⚠️⚠️ Alerte 2 — Deuxième avertissement";
        String body = buildWarning2Body(userKey, badWord);
        return sendEmail(toEmail, subject, body);
    }

    public boolean sendBlockNotification(String toEmail, String userKey, String badWord, int blockHours) {
        String subject = "🚫 Compte bloqué temporairement — Troisième infraction";
        String body = buildBlockBody(userKey, badWord, blockHours);
        return sendEmail(toEmail, subject, body);
    }

    public boolean sendEmail(String toEmail, String subject, String htmlBody) {
        if (!enabled) {
            System.out.println("[ForumEmailService] Email désactivé. Sujet: " + subject);
            return false;
        }
        if (senderEmail.isEmpty() || senderPassword.isEmpty()) {
            System.err.println("[ForumEmailService] Configuration SMTP manquante (email/mot de passe).");
            return false;
        }
        if (toEmail == null || toEmail.isBlank()) {
            System.err.println("[ForumEmailService] Adresse destinataire vide.");
            return false;
        }

        try {
            Properties props = new Properties();
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.host", smtpHost);
            props.put("mail.smtp.port", String.valueOf(smtpPort));
            props.put("mail.smtp.ssl.trust", smtpHost);

            Session session = Session.getInstance(props, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(senderEmail, senderPassword);
                }
            });

            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(senderEmail, "Tabaani Modération"));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
            message.setSubject(subject);
            message.setContent(htmlBody, "text/html; charset=UTF-8");

            Transport.send(message);
            System.out.println("[ForumEmailService] ✅ Email envoyé à " + toEmail + " — " + subject);
            return true;

        } catch (Exception e) {
            System.err.println("[ForumEmailService] ❌ Erreur d'envoi: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private String buildWarning1Body(String userKey, String badWord) {
        return """
            <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; background: #1a1a2e; color: #e0e0e0; padding: 30px; border-radius: 12px;">
                <h2 style="color: #FFD700;">⚠️ Premier Avertissement</h2>
                <p>Bonjour <strong style="color: #FFB347;">%s</strong>,</p>
                <p>Nous avons détecté l'utilisation d'un <strong>mot inapproprié</strong> dans votre publication :</p>
                <div style="background: #2d2d44; padding: 12px 20px; border-radius: 8px; border-left: 4px solid #FFD700; margin: 16px 0;">
                    <strong>Mot détecté :</strong> <code style="color: #FF6B6B;">%s</code>
                </div>
                <p>Ceci est votre <strong style="color: #FFD700;">premier avertissement</strong>. Veuillez respecter les règles de la communauté.</p>
                <div style="background: #2d2d44; padding: 12px; border-radius: 8px; margin-top: 16px;">
                    <small style="color: #888;">
                        📊 <strong>Système de modération :</strong><br/>
                        ✅ Alerte 1 — Avertissement (actuel)<br/>
                        ⚠️ Alerte 2 — Deuxième avertissement<br/>
                        🚫 Alerte 3 — Blocage temporaire du compte
                    </small>
                </div>
                <p style="margin-top: 20px; color: #888; font-size: 12px;">— Équipe Tabaani Connect</p>
            </div>
            """.formatted(userKey, badWord != null ? badWord : "***");
    }

    private String buildWarning2Body(String userKey, String badWord) {
        return """
            <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; background: #1a1a2e; color: #e0e0e0; padding: 30px; border-radius: 12px;">
                <h2 style="color: #FF8C00;">⚠️⚠️ Deuxième Avertissement</h2>
                <p>Bonjour <strong style="color: #FFB347;">%s</strong>,</p>
                <p>Vous avez utilisé un <strong>mot inapproprié</strong> pour la <strong style="color: #FF8C00;">deuxième fois</strong> :</p>
                <div style="background: #2d2d44; padding: 12px 20px; border-radius: 8px; border-left: 4px solid #FF8C00; margin: 16px 0;">
                    <strong>Mot détecté :</strong> <code style="color: #FF6B6B;">%s</code>
                </div>
                <p style="color: #FF8C00;"><strong>⚠️ ATTENTION :</strong> C'est votre dernier avertissement avant le blocage temporaire de votre compte.</p>
                <p>Une prochaine infraction entraînera un <strong>blocage de 24 heures</strong>.</p>
                <div style="background: #2d2d44; padding: 12px; border-radius: 8px; margin-top: 16px;">
                    <small style="color: #888;">
                        📊 <strong>Votre historique :</strong><br/>
                        ✅ Alerte 1 — Déjà reçu<br/>
                        ⚠️ Alerte 2 — Actuel<br/>
                        🚫 Alerte 3 — Prochain = Blocage !
                    </small>
                </div>
                <p style="margin-top: 20px; color: #888; font-size: 12px;">— Équipe Tabaani Connect</p>
            </div>
            """.formatted(userKey, badWord != null ? badWord : "***");
    }

    private String buildBlockBody(String userKey, String badWord, int blockHours) {
        return """
            <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; background: #1a1a2e; color: #e0e0e0; padding: 30px; border-radius: 12px;">
                <h2 style="color: #FF4444;">🚫 Compte Bloqué Temporairement</h2>
                <p>Bonjour <strong style="color: #FFB347;">%s</strong>,</p>
                <p>Suite à votre <strong style="color: #FF4444;">troisième infraction</strong>, votre compte a été <strong>bloqué temporairement</strong>.</p>
                <div style="background: #2d2d44; padding: 12px 20px; border-radius: 8px; border-left: 4px solid #FF4444; margin: 16px 0;">
                    <strong>Mot détecté :</strong> <code style="color: #FF6B6B;">%s</code><br/>
                    <strong>Durée du blocage :</strong> <span style="color: #FF4444;">%d heures</span>
                </div>
                <p>Vous ne pourrez pas publier de contenu pendant cette période.</p>
                <p>Après le déblocage, toute nouvelle infraction entraînera un blocage plus long.</p>
                <div style="background: #2d2d44; padding: 12px; border-radius: 8px; margin-top: 16px; border: 1px solid #FF4444;">
                    <small style="color: #FF8C00;">
                        ⚠️ Les infractions répétées peuvent mener à un blocage permanent.
                    </small>
                </div>
                <p style="margin-top: 20px; color: #888; font-size: 12px;">— Équipe Tabaani Connect</p>
            </div>
            """.formatted(userKey, badWord != null ? badWord : "***", blockHours);
    }
}


