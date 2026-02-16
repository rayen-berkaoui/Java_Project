package com.esprit.services;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.mail.*;
import javax.mail.internet.*;
import javax.mail.util.ByteArrayDataSource;
import java.io.InputStream;
import java.util.Properties;

public class EmailService {

    private static final String SMTP_HOST = "smtp.gmail.com";
    private static final int SMTP_PORT = 587;
    private static final String EMAIL_FROM = "dragona.berkaoui@gmail.com";
    private static final String EMAIL_PASSWORD = "felx qgrf acbe lbii";

    // Cache the logo bytes so we only read from resources once
    private static byte[] logoBytesCache = null;

    private Session getMailSession() {
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", SMTP_HOST);
        props.put("mail.smtp.port", String.valueOf(SMTP_PORT));
        props.put("mail.smtp.ssl.trust", SMTP_HOST);
        return Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(EMAIL_FROM, EMAIL_PASSWORD);
            }
        });
    }

    private byte[] getLogoBytes() {
        if (logoBytesCache != null) return logoBytesCache;
        try {
            InputStream is = getClass().getResourceAsStream("/loginback.png");
            if (is != null) {
                logoBytesCache = is.readAllBytes();
                is.close();
                System.out.println("✅ Logo loaded from resources (" + logoBytesCache.length + " bytes)");
            }
        } catch (Exception e) {
            System.out.println("⚠ Failed to load logo: " + e.getMessage());
        }
        return logoBytesCache;
    }

    /**
     * Send a professional OTP email with the Tabaani logo attached inline via CID
     */
    public boolean sendOtpEmail(String toEmail, String otpCode) {
        try {
            Properties props = new Properties();
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.host", SMTP_HOST);
            props.put("mail.smtp.port", String.valueOf(SMTP_PORT));
            props.put("mail.smtp.ssl.trust", SMTP_HOST);

            Session session = Session.getInstance(props, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(EMAIL_FROM, EMAIL_PASSWORD);
                }
            });

            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(EMAIL_FROM, "Tabaani - Support"));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
            message.setSubject("\uD83D\uDD10 Tabaani - Code de vérification");

            // Use multipart/related so CID references work
            MimeMultipart multipart = new MimeMultipart("related");

            // Part 1: HTML body
            MimeBodyPart htmlPart = new MimeBodyPart();
            htmlPart.setContent(buildHtmlEmail(otpCode), "text/html; charset=UTF-8");
            multipart.addBodyPart(htmlPart);

            // Part 2: Logo image as CID inline attachment
            byte[] logoBytes = getLogoBytes();
            if (logoBytes != null) {
                MimeBodyPart logoPart = new MimeBodyPart();
                DataSource logoDs = new ByteArrayDataSource(logoBytes, "image/png");
                logoPart.setDataHandler(new DataHandler(logoDs));
                logoPart.setHeader("Content-ID", "<tabaani-logo>");
                logoPart.setFileName("tabaani_logo.png");
                logoPart.setDisposition(MimeBodyPart.INLINE);
                multipart.addBodyPart(logoPart);
                System.out.println("✅ Logo attached as CID inline");
            } else {
                System.out.println("⚠ Logo not found, sending without image");
            }

            message.setContent(multipart);
            Transport.send(message);
            System.out.println("✅ OTP email sent to: " + toEmail);
            return true;

        } catch (Exception e) {
            System.out.println("❌ Failed to send email: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Build professional HTML email — logo referenced via cid:tabaani-logo
     */
    private String buildHtmlEmail(String otpCode) {
        return "<!DOCTYPE html>\n" +
                "<html lang=\"fr\">\n" +
                "<head><meta charset=\"UTF-8\"><meta name=\"viewport\" content=\"width=device-width,initial-scale=1.0\"></head>\n" +
                "<body style=\"margin:0;padding:0;background-color:#0a0a0a;font-family:'Segoe UI',Arial,sans-serif;\">\n" +
                "\n" +
                "<table role=\"presentation\" width=\"100%\" cellspacing=\"0\" cellpadding=\"0\" style=\"background-color:#0a0a0a;\">\n" +
                "  <tr><td align=\"center\" style=\"padding:40px 20px;\">\n" +
                "\n" +
                "    <!-- Main Card -->\n" +
                "    <table role=\"presentation\" width=\"520\" cellspacing=\"0\" cellpadding=\"0\"\n" +
                "           style=\"background-color:#111111;border-radius:16px;border:1px solid rgba(255,215,0,0.2);\">\n" +
                "\n" +
                "      <!-- Header with Logo (CID inline attachment) -->\n" +
                "      <tr>\n" +
                "        <td align=\"center\" style=\"padding:35px 40px 15px 40px;\">\n" +
                "          <img src=\"cid:tabaani-logo\" alt=\"Tabaani\" width=\"130\"\n" +
                "               style=\"display:block;border:0;max-width:130px;border-radius:12px;\" />\n" +
                "        </td>\n" +
                "      </tr>\n" +
                "\n" +
                "      <!-- Gold Accent Line -->\n" +
                "      <tr>\n" +
                "        <td style=\"padding:0 60px;\">\n" +
                "          <div style=\"height:2px;background:linear-gradient(90deg,transparent,#FFD700,transparent);\"></div>\n" +
                "        </td>\n" +
                "      </tr>\n" +
                "\n" +
                "      <!-- Title Section -->\n" +
                "      <tr>\n" +
                "        <td align=\"center\" style=\"padding:28px 40px 8px 40px;\">\n" +
                "          <h1 style=\"margin:0;color:#FFD700;font-size:22px;font-weight:700;letter-spacing:0.5px;\">\n" +
                "            R\u00e9initialisation du mot de passe\n" +
                "          </h1>\n" +
                "        </td>\n" +
                "      </tr>\n" +
                "      <tr>\n" +
                "        <td align=\"center\" style=\"padding:0 40px 5px 40px;\">\n" +
                "          <p style=\"margin:0;color:#aaaaaa;font-size:14px;line-height:1.6;\">\n" +
                "            Bonjour,<br>Nous avons re\u00e7u une demande de r\u00e9initialisation de votre mot de passe.\n" +
                "            Veuillez utiliser le code ci-dessous.\n" +
                "          </p>\n" +
                "        </td>\n" +
                "      </tr>\n" +
                "\n" +
                "      <!-- OTP Code Box -->\n" +
                "      <tr>\n" +
                "        <td align=\"center\" style=\"padding:28px 40px;\">\n" +
                "          <table role=\"presentation\" cellspacing=\"0\" cellpadding=\"0\">\n" +
                "            <tr>\n" +
                "              <td style=\"background-color:#0a0a0a;border:2px solid #FFD700;border-radius:14px;padding:22px 55px;\">\n" +
                "                <span style=\"font-size:38px;font-weight:800;color:#FFD700;letter-spacing:14px;font-family:'Courier New',monospace;\">" +
                otpCode +
                "</span>\n" +
                "              </td>\n" +
                "            </tr>\n" +
                "          </table>\n" +
                "        </td>\n" +
                "      </tr>\n" +
                "\n" +
                "      <!-- Timer Warning -->\n" +
                "      <tr>\n" +
                "        <td align=\"center\" style=\"padding:0 40px 20px 40px;\">\n" +
                "          <table role=\"presentation\" cellspacing=\"0\" cellpadding=\"0\">\n" +
                "            <tr>\n" +
                "              <td style=\"background-color:rgba(255,107,107,0.08);border:1px solid rgba(255,107,107,0.2);border-radius:10px;padding:12px 28px;\">\n" +
                "                <span style=\"color:#FF6B6B;font-size:13px;font-weight:600;\">\n" +
                "                  \u23F1 Ce code expire dans 60 secondes\n" +
                "                </span>\n" +
                "              </td>\n" +
                "            </tr>\n" +
                "          </table>\n" +
                "        </td>\n" +
                "      </tr>\n" +
                "\n" +
                "      <!-- Divider -->\n" +
                "      <tr>\n" +
                "        <td style=\"padding:0 40px;\">\n" +
                "          <div style=\"height:1px;background-color:rgba(255,255,255,0.06);\"></div>\n" +
                "        </td>\n" +
                "      </tr>\n" +
                "\n" +
                "      <!-- Security Notice -->\n" +
                "      <tr>\n" +
                "        <td align=\"center\" style=\"padding:20px 40px;\">\n" +
                "          <p style=\"margin:0;color:#555555;font-size:12px;line-height:1.7;\">\n" +
                "            \uD83D\uDD12 Si vous n'avez pas demand\u00e9 ce code, vous pouvez ignorer cet email en toute s\u00e9curit\u00e9.<br>\n" +
                "            Ne partagez jamais ce code avec qui que ce soit.\n" +
                "          </p>\n" +
                "        </td>\n" +
                "      </tr>\n" +
                "\n" +
                "      <!-- Footer -->\n" +
                "      <tr>\n" +
                "        <td align=\"center\"\n" +
                "            style=\"padding:18px 40px;background-color:#0a0a0a;border-top:1px solid rgba(255,215,0,0.08);border-radius:0 0 16px 16px;\">\n" +
                "          <p style=\"margin:0;color:#444444;font-size:11px;\">\n" +
                "            \u00A9 2026 Tabaani \u2014 Tous droits r\u00e9serv\u00e9s\n" +
                "          </p>\n" +
                "          <p style=\"margin:4px 0 0 0;color:#333333;font-size:10px;\">\n" +
                "            Cet email a \u00e9t\u00e9 envoy\u00e9 automatiquement \u2022 Ne pas r\u00e9pondre\n" +
                "          </p>\n" +
                "        </td>\n" +
                "      </tr>\n" +
                "\n" +
                "    </table>\n" +
                "  </td></tr>\n" +
                "</table>\n" +
                "</body>\n" +
                "</html>";
    }

    /**
     * Send payment confirmation email with receipt details
     */
    public boolean sendPaymentConfirmationEmail(String toEmail, String customerName,
            String serviceName, String confirmationCode, double amount,
            String paymentMethod, int loyaltyPointsEarned, int totalLoyaltyPoints) {
        try {
            Session session = getMailSession();

            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(EMAIL_FROM, "Tabaani - SmartTravel"));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
            message.setSubject("\u2705 Tabaani - Confirmation de paiement #" + confirmationCode);

            MimeMultipart multipart = new MimeMultipart("related");

            MimeBodyPart htmlPart = new MimeBodyPart();
            htmlPart.setContent(buildPaymentConfirmationHtml(customerName, serviceName,
                    confirmationCode, amount, paymentMethod, loyaltyPointsEarned, totalLoyaltyPoints),
                    "text/html; charset=UTF-8");
            multipart.addBodyPart(htmlPart);

            byte[] logoBytes = getLogoBytes();
            if (logoBytes != null) {
                MimeBodyPart logoPart = new MimeBodyPart();
                DataSource logoDs = new ByteArrayDataSource(logoBytes, "image/png");
                logoPart.setDataHandler(new DataHandler(logoDs));
                logoPart.setHeader("Content-ID", "<tabaani-logo>");
                logoPart.setFileName("tabaani_logo.png");
                logoPart.setDisposition(MimeBodyPart.INLINE);
                multipart.addBodyPart(logoPart);
            }

            message.setContent(multipart);
            Transport.send(message);
            System.out.println("\u2705 Payment confirmation email sent to: " + toEmail);
            return true;
        } catch (Exception e) {
            System.out.println("\u274C Failed to send payment email: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private String buildPaymentConfirmationHtml(String customerName, String serviceName,
            String code, double amount, String paymentMethod,
            int pointsEarned, int totalPoints) {
        String loyaltySection = pointsEarned > 0 ?
            "      <tr>\n" +
            "        <td style=\"padding:0 40px 20px 40px;\">\n" +
            "          <table width=\"100%\" cellspacing=\"0\" cellpadding=\"0\" style=\"background-color:rgba(255,215,0,0.06);border:1px solid rgba(255,215,0,0.15);border-radius:12px;\">\n" +
            "            <tr><td style=\"padding:16px 24px;\">\n" +
            "              <p style=\"margin:0;color:#FFD700;font-size:14px;font-weight:700;\">\u2B50 Points de fid\u00e9lit\u00e9</p>\n" +
            "              <p style=\"margin:6px 0 0 0;color:#aaa;font-size:13px;\">+" + pointsEarned + " points gagn\u00e9s avec ce paiement</p>\n" +
            "              <p style=\"margin:4px 0 0 0;color:#FFD700;font-size:15px;font-weight:700;\">Solde total: " + totalPoints + " points</p>\n" +
            "            </td></tr>\n" +
            "          </table>\n" +
            "        </td>\n" +
            "      </tr>\n" : "";

        return "<!DOCTYPE html>\n<html lang=\"fr\">\n" +
            "<head><meta charset=\"UTF-8\"></head>\n" +
            "<body style=\"margin:0;padding:0;background-color:#0a0a0a;font-family:'Segoe UI',Arial,sans-serif;\">\n" +
            "<table role=\"presentation\" width=\"100%\" cellspacing=\"0\" cellpadding=\"0\" style=\"background-color:#0a0a0a;\">\n" +
            "  <tr><td align=\"center\" style=\"padding:40px 20px;\">\n" +
            "    <table role=\"presentation\" width=\"520\" cellspacing=\"0\" cellpadding=\"0\" style=\"background-color:#111111;border-radius:16px;border:1px solid rgba(255,215,0,0.2);\">\n" +
            "      <tr><td align=\"center\" style=\"padding:30px 40px 15px;\"><img src=\"cid:tabaani-logo\" alt=\"Tabaani\" width=\"100\" style=\"border-radius:10px;\"/></td></tr>\n" +
            "      <tr><td style=\"padding:0 60px;\"><div style=\"height:2px;background:linear-gradient(90deg,transparent,#51CF66,transparent);\"></div></td></tr>\n" +
            "      <tr><td align=\"center\" style=\"padding:24px 40px 8px;\"><h1 style=\"margin:0;color:#51CF66;font-size:22px;\">\u2705 Paiement Confirm\u00e9</h1></td></tr>\n" +
            "      <tr><td align=\"center\" style=\"padding:0 40px 20px;\"><p style=\"margin:0;color:#aaa;font-size:14px;\">Bonjour " + customerName + ",<br>Votre paiement a \u00e9t\u00e9 trait\u00e9 avec succ\u00e8s.</p></td></tr>\n" +
            "      <tr><td align=\"center\" style=\"padding:0 40px 20px;\">\n" +
            "        <table cellspacing=\"0\" cellpadding=\"0\"><tr><td style=\"background-color:#0a0a0a;border:2px solid #FFD700;border-radius:14px;padding:16px 40px;\">\n" +
            "          <span style=\"font-size:28px;font-weight:800;color:#FFD700;letter-spacing:6px;font-family:'Courier New',monospace;\">" + code + "</span>\n" +
            "        </td></tr></table>\n" +
            "      </td></tr>\n" +
            "      <tr><td style=\"padding:0 40px 20px;\">\n" +
            "        <table width=\"100%\" cellspacing=\"0\" cellpadding=\"0\" style=\"background-color:rgba(255,255,255,0.03);border-radius:12px;border:1px solid rgba(255,255,255,0.06);\">\n" +
            "          <tr><td style=\"padding:14px 20px;border-bottom:1px solid rgba(255,255,255,0.04);\">\n" +
            "            <span style=\"color:#888;font-size:12px;\">Service</span><br/><span style=\"color:white;font-size:14px;font-weight:600;\">" + serviceName + "</span></td></tr>\n" +
            "          <tr><td style=\"padding:14px 20px;border-bottom:1px solid rgba(255,255,255,0.04);\">\n" +
            "            <span style=\"color:#888;font-size:12px;\">Mode de paiement</span><br/><span style=\"color:white;font-size:14px;font-weight:600;\">" + paymentMethod + "</span></td></tr>\n" +
            "          <tr><td style=\"padding:14px 20px;\">\n" +
            "            <span style=\"color:#888;font-size:12px;\">Montant total</span><br/><span style=\"color:#FFD700;font-size:20px;font-weight:800;\">" + String.format("%.2f DT", amount) + "</span></td></tr>\n" +
            "        </table>\n" +
            "      </td></tr>\n" +
            loyaltySection +
            "      <tr><td style=\"padding:0 40px;\"><div style=\"height:1px;background-color:rgba(255,255,255,0.06);\"></div></td></tr>\n" +
            "      <tr><td align=\"center\" style=\"padding:16px 40px;background-color:#0a0a0a;border-top:1px solid rgba(255,215,0,0.08);border-radius:0 0 16px 16px;\">\n" +
            "        <p style=\"margin:0;color:#444;font-size:11px;\">\u00A9 2026 Tabaani \u2014 SmartTravel</p>\n" +
            "        <p style=\"margin:4px 0 0 0;color:#333;font-size:10px;\">Cet email a \u00e9t\u00e9 envoy\u00e9 automatiquement</p>\n" +
            "      </td></tr>\n" +
            "    </table>\n" +
            "  </td></tr>\n</table>\n</body>\n</html>";
    }
}
