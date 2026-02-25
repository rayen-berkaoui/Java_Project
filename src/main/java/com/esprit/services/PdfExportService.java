package com.esprit.services;

import com.esprit.entities.Activite;
import com.esprit.entities.Etablissement;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.itextpdf.io.image.ImageData;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.action.PdfAction;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.element.Text;
import com.itextpdf.layout.properties.HorizontalAlignment;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.layout.properties.VerticalAlignment;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Service d'exportation PDF avancé pour l'agence de voyage.
 * Génère des PDFs premium avec :
 * - Design noir & or moderne
 * - QR Codes Google Maps intégrés
 * - Carte statique Google Maps
 * - Itinéraire personnalisé auto-généré
 * - Statistiques visuelles
 */
public class PdfExportService {

    // ===== COULEURS THÈME PREMIUM =====
    private static final DeviceRgb GOLD = new DeviceRgb(255, 193, 7);
    private static final DeviceRgb DARK_BG = new DeviceRgb(24, 24, 32);
    private static final DeviceRgb CARD_BG = new DeviceRgb(35, 35, 48);
    private static final DeviceRgb LIGHT_TEXT = new DeviceRgb(220, 220, 230);
    private static final DeviceRgb MUTED_TEXT = new DeviceRgb(160, 160, 175);
    private static final DeviceRgb ACCENT_BLUE = new DeviceRgb(66, 133, 244);
    private static final DeviceRgb SUCCESS_GREEN = new DeviceRgb(76, 175, 80);
    private static final DeviceRgb WARM_ORANGE = new DeviceRgb(255, 152, 0);

    private static final SimpleDateFormat DATE_FMT = new SimpleDateFormat("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter NOW_FMT = DateTimeFormatter.ofPattern("dd MMMM yyyy 'à' HH:mm", Locale.FRENCH);

    // ===================================================================
    //  EXPORT ÉTABLISSEMENT → PDF
    // ===================================================================
    public static void exportEtablissement(Etablissement etab, File outputFile) throws Exception {
        PdfWriter writer = new PdfWriter(outputFile);
        PdfDocument pdf = new PdfDocument(writer);
        Document doc = new Document(pdf, PageSize.A4);
        doc.setMargins(30, 35, 30, 35);

        PdfFont bold = PdfFontFactory.createFont("Helvetica-Bold");
        PdfFont regular = PdfFontFactory.createFont("Helvetica");

        // Background
        addPageBackground(pdf);

        // ===== HEADER =====
        addPremiumHeader(doc, bold, regular, "FICHE ÉTABLISSEMENT");

        // ===== HERO SECTION =====
        addEtablissementHero(doc, etab, bold, regular);

        // ===== INFORMATIONS DÉTAILLÉES =====
        addSectionTitle(doc, bold, "\u2139\uFE0F  INFORMATIONS DÉTAILLÉES");
        addEtablissementInfoTable(doc, etab, bold, regular);

        // ===== DESCRIPTION =====
        if (etab.getDescription() != null && !etab.getDescription().isBlank()) {
            addSectionTitle(doc, bold, "\uD83D\uDCDD  DESCRIPTION");
            addDescriptionCard(doc, etab.getDescription(), regular);
        }

        // ===== ACTIVITÉS ASSOCIÉES =====
        List<Activite> activites = getActivitesByEtablissement(etab.getIdEtablissement());
        if (!activites.isEmpty()) {
            addSectionTitle(doc, bold, "\uD83C\uDFAF  ACTIVITÉS PROPOSÉES (" + activites.size() + ")");
            addActivitesTable(doc, activites, bold, regular);
        }

        // ===== ITINÉRAIRE PERSONNALISÉ =====
        if (!activites.isEmpty()) {
            addSectionTitle(doc, bold, "\uD83D\uDDFA\uFE0F  ITINÉRAIRE SUGGÉRÉ");
            addItinerary(doc, etab, activites, bold, regular);
        }

        // ===== STATISTIQUES =====
        addSectionTitle(doc, bold, "\uD83D\uDCCA  STATISTIQUES");
        addStatisticsCard(doc, etab, activites, bold, regular);

        // ===== LOCALISATION + QR CODE =====
        addSectionTitle(doc, bold, "\uD83D\uDCCD  LOCALISATION & QR CODE");
        addLocationSection(doc, etab, bold, regular);

        // ===== FOOTER =====
        addFooter(doc, regular);

        doc.close();
    }

    // ===================================================================
    //  EXPORT ACTIVITÉ → PDF
    // ===================================================================
    public static void exportActivite(Activite act, File outputFile) throws Exception {
        PdfWriter writer = new PdfWriter(outputFile);
        PdfDocument pdf = new PdfDocument(writer);
        Document doc = new Document(pdf, PageSize.A4);
        doc.setMargins(30, 35, 30, 35);

        PdfFont bold = PdfFontFactory.createFont("Helvetica-Bold");
        PdfFont regular = PdfFontFactory.createFont("Helvetica");

        addPageBackground(pdf);

        // ===== HEADER =====
        addPremiumHeader(doc, bold, regular, "FICHE ACTIVITÉ");

        // ===== HERO =====
        addActiviteHero(doc, act, bold, regular);

        // ===== INFORMATIONS =====
        addSectionTitle(doc, bold, "\u2139\uFE0F  INFORMATIONS DÉTAILLÉES");
        addActiviteInfoTable(doc, act, bold, regular);

        // ===== DESCRIPTION =====
        if (act.getDescription() != null && !act.getDescription().isBlank()) {
            addSectionTitle(doc, bold, "\uD83D\uDCDD  DESCRIPTION");
            addDescriptionCard(doc, act.getDescription(), regular);
        }

        // ===== ÉQUIPEMENT & CONDITIONS =====
        if ((act.getEquipementInclus() != null && !act.getEquipementInclus().isBlank())
                || (act.getConditionsAnnulation() != null && !act.getConditionsAnnulation().isBlank())) {
            addSectionTitle(doc, bold, "\uD83C\uDFD2  ÉQUIPEMENT & CONDITIONS");
            addEquipmentCard(doc, act, bold, regular);
        }

        // ===== TARIFICATION =====
        addSectionTitle(doc, bold, "\uD83D\uDCB0  TARIFICATION & DISPONIBILITÉ");
        addPricingCard(doc, act, bold, regular);

        // ===== ITINÉRAIRE MONO-ACTIVITÉ =====
        addSectionTitle(doc, bold, "\uD83D\uDDFA\uFE0F  PROGRAMME DE VISITE");
        addSingleActivityItinerary(doc, act, bold, regular);

        // ===== LOCALISATION + QR =====
        if (act.getAdresseDepart() != null && !act.getAdresseDepart().isBlank()) {
            addSectionTitle(doc, bold, "\uD83D\uDCCD  LOCALISATION & QR CODE");
            addActivityLocationSection(doc, act, bold, regular);
        }

        // ===== FOOTER =====
        addFooter(doc, regular);

        doc.close();
    }

    // ===================================================================
    //  COMPOSANTS RÉUTILISABLES
    // ===================================================================

    private static void addPageBackground(PdfDocument pdf) {
        // Dark background per page (via rectangle annotation on each page)
        for (int i = 1; i <= pdf.getNumberOfPages(); i++) {
            var page = pdf.getPage(i);
            var canvas = new com.itextpdf.kernel.pdf.canvas.PdfCanvas(page.newContentStreamBefore(), page.getResources(), pdf);
            canvas.saveState()
                    .setFillColor(DARK_BG)
                    .rectangle(0, 0, page.getPageSize().getWidth(), page.getPageSize().getHeight())
                    .fill()
                    .restoreState();
        }
    }

    private static void addPremiumHeader(Document doc, PdfFont bold, PdfFont regular, String subtitle) {
        // Gold line
        Table headerLine = new Table(1).useAllAvailableWidth();
        headerLine.addCell(new Cell().setBackgroundColor(GOLD).setHeight(3).setBorder(Border.NO_BORDER));
        doc.add(headerLine);

        doc.add(new Paragraph("\u2728 Premium Travel Agency")
                .setFont(bold).setFontSize(24).setFontColor(GOLD)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginTop(12).setMarginBottom(2));

        doc.add(new Paragraph(subtitle)
                .setFont(regular).setFontSize(11).setFontColor(MUTED_TEXT)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(4));

        doc.add(new Paragraph("Généré le " + LocalDateTime.now().format(NOW_FMT))
                .setFont(regular).setFontSize(9).setFontColor(MUTED_TEXT)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(16));

        // Gold line
        Table footerLine = new Table(1).useAllAvailableWidth();
        footerLine.addCell(new Cell().setBackgroundColor(GOLD).setHeight(1).setBorder(Border.NO_BORDER));
        doc.add(footerLine);
    }

    private static void addSectionTitle(Document doc, PdfFont bold, String title) {
        Paragraph p = new Paragraph(title)
                .setFont(bold).setFontSize(14).setFontColor(GOLD)
                .setMarginTop(20).setMarginBottom(8)
                .setBorderBottom(new SolidBorder(GOLD, 0.5f))
                .setPaddingBottom(4);
        doc.add(p);
    }

    // ===== ÉTABLISSEMENT COMPONENTS =====

    private static void addEtablissementHero(Document doc, Etablissement e, PdfFont bold, PdfFont regular) {
        Table hero = new Table(UnitValue.createPercentArray(new float[]{70, 30})).useAllAvailableWidth();
        hero.setBackgroundColor(CARD_BG).setPadding(16).setMarginTop(14);
        hero.setBorderRadius(new com.itextpdf.layout.properties.BorderRadius(8));

        // Left: name + type
        Cell left = new Cell().setBorder(Border.NO_BORDER);
        left.add(new Paragraph(safe(e.getNom()))
                .setFont(bold).setFontSize(20).setFontColor(ColorConstants.WHITE));

        StringBuilder sub = new StringBuilder();
        if (!safe(e.getType()).isBlank()) sub.append(safe(e.getType()).toUpperCase());
        if (!safe(e.getVille()).isBlank()) {
            if (sub.length() > 0) sub.append("  •  ");
            sub.append(safe(e.getVille()));
        }
        if (!safe(e.getGammePrix()).isBlank()) {
            if (sub.length() > 0) sub.append("  •  ");
            sub.append(safe(e.getGammePrix()));
        }
        left.add(new Paragraph(sub.toString())
                .setFont(regular).setFontSize(11).setFontColor(GOLD).setMarginTop(4));

        hero.addCell(left);

        // Right: type badge
        Cell right = new Cell().setBorder(Border.NO_BORDER)
                .setVerticalAlignment(VerticalAlignment.MIDDLE)
                .setTextAlignment(TextAlignment.RIGHT);

        String typeEmoji = getTypeEmoji(safe(e.getType()));
        right.add(new Paragraph(typeEmoji)
                .setFontSize(36).setTextAlignment(TextAlignment.RIGHT));
        hero.addCell(right);

        doc.add(hero);
    }

    private static void addEtablissementInfoTable(Document doc, Etablissement e, PdfFont bold, PdfFont regular) {
        Table table = new Table(UnitValue.createPercentArray(new float[]{35, 65})).useAllAvailableWidth();
        table.setBackgroundColor(CARD_BG).setPadding(12);

        addInfoRow(table, "Adresse", safe(e.getAdresse()), bold, regular);
        addInfoRow(table, "Ville", safe(e.getVille()), bold, regular);
        addInfoRow(table, "Téléphone", safe(e.getTelephone()), bold, regular);
        addInfoRow(table, "Email", safe(e.getEmail()), bold, regular);
        addInfoRow(table, "Horaires", safe(e.getHoraires()), bold, regular);
        addInfoRow(table, "Gamme Prix", safe(e.getGammePrix()), bold, regular);
        addInfoRow(table, "Type", safe(e.getType()), bold, regular);

        doc.add(table);
    }

    private static void addActivitesTable(Document doc, List<Activite> activites, PdfFont bold, PdfFont regular) {
        Table table = new Table(UnitValue.createPercentArray(new float[]{30, 18, 15, 15, 22})).useAllAvailableWidth();
        table.setBackgroundColor(CARD_BG);

        // Header
        String[] headers = {"Activité", "Catégorie", "Niveau", "Durée", "Prix"};
        for (String h : headers) {
            table.addHeaderCell(new Cell()
                    .add(new Paragraph(h).setFont(bold).setFontSize(9).setFontColor(GOLD))
                    .setBackgroundColor(new DeviceRgb(45, 45, 60))
                    .setBorder(new SolidBorder(new DeviceRgb(60, 60, 80), 0.5f))
                    .setPadding(6));
        }

        for (Activite a : activites) {
            addTableCell(table, safe(a.getNomActivite()), regular);
            addTableCell(table, safe(a.getCategorie()), regular);
            addTableCell(table, safe(a.getNiveau()), regular);
            addTableCell(table, a.getDuree() != null ? a.getDuree() + " min" : "—", regular);

            String prix = "—";
            if (a.getPrix() != null) {
                prix = a.getPrix().stripTrailingZeros().toPlainString() + " " + safe(a.getDevise());
            }
            addTableCell(table, prix, regular);
        }

        doc.add(table);
    }

    private static void addItinerary(Document doc, Etablissement etab, List<Activite> activites, PdfFont bold, PdfFont regular) {
        Table itinerary = new Table(UnitValue.createPercentArray(new float[]{15, 85})).useAllAvailableWidth();
        itinerary.setBackgroundColor(CARD_BG).setPadding(14);

        // Step 0: Arrival
        addItineraryStep(itinerary, "09:00", "\uD83D\uDEEC  Arrivée à " + safe(etab.getNom()),
                "Accueil et briefing. Adresse : " + safe(etab.getAdresse()) + ", " + safe(etab.getVille()),
                bold, regular, ACCENT_BLUE);

        // Generate time slots for activities
        int hour = 9;
        int minute = 30;
        for (int i = 0; i < activites.size(); i++) {
            Activite a = activites.get(i);
            String time = String.format("%02d:%02d", hour, minute);

            int duration = a.getDuree() != null ? a.getDuree() : 60;
            String durText = duration + " min";

            String icon = getCategoryIcon(safe(a.getCategorie()));
            String details = "Catégorie : " + safe(a.getCategorie());
            if (!safe(a.getNiveau()).isBlank()) details += " | Niveau : " + a.getNiveau();
            if (a.getPrix() != null) details += " | Prix : " + a.getPrix().stripTrailingZeros().toPlainString() + " " + safe(a.getDevise());
            details += " | Durée : " + durText;

            addItineraryStep(itinerary, time, icon + "  " + safe(a.getNomActivite()),
                    details, bold, regular, GOLD);

            // Advance time
            minute += duration;
            while (minute >= 60) { hour++; minute -= 60; }

            // Add break between activities
            if (i < activites.size() - 1) {
                String breakTime = String.format("%02d:%02d", hour, minute);
                addItineraryStep(itinerary, breakTime, "\u2615  Pause détente",
                        "Temps libre pour se reposer (15 min)", bold, regular, MUTED_TEXT);
                minute += 15;
                while (minute >= 60) { hour++; minute -= 60; }
            }
        }

        // Final step
        String endTime = String.format("%02d:%02d", hour, minute + 15);
        addItineraryStep(itinerary, endTime, "\uD83C\uDF1F  Fin de la visite",
                "Merci pour votre visite ! N'oubliez pas de laisser un avis.",
                bold, regular, SUCCESS_GREEN);

        doc.add(itinerary);
    }

    private static void addItineraryStep(Table table, String time, String title, String details,
                                          PdfFont bold, PdfFont regular, DeviceRgb color) {
        Cell timeCell = new Cell()
                .add(new Paragraph(time).setFont(bold).setFontSize(10).setFontColor(color))
                .setBorder(Border.NO_BORDER)
                .setBorderRight(new SolidBorder(new DeviceRgb(60, 60, 80), 1))
                .setPadding(6)
                .setVerticalAlignment(VerticalAlignment.TOP);
        table.addCell(timeCell);

        Cell contentCell = new Cell().setBorder(Border.NO_BORDER).setPadding(6);
        contentCell.add(new Paragraph(title).setFont(bold).setFontSize(10).setFontColor(ColorConstants.WHITE));
        contentCell.add(new Paragraph(details).setFont(regular).setFontSize(8).setFontColor(MUTED_TEXT).setMarginTop(2));
        table.addCell(contentCell);
    }

    private static void addStatisticsCard(Document doc, Etablissement etab, List<Activite> activites, PdfFont bold, PdfFont regular) {
        Table stats = new Table(UnitValue.createPercentArray(new float[]{25, 25, 25, 25})).useAllAvailableWidth();
        stats.setBackgroundColor(CARD_BG).setPadding(14);

        // Calculate stats
        int totalActivites = activites.size();
        int totalPlaces = activites.stream().mapToInt(a -> a.getNbPlaces() != null ? a.getNbPlaces() : 0).sum();
        int totalDispo = activites.stream().mapToInt(a -> a.getPlacesDispo() != null ? a.getPlacesDispo() : 0).sum();

        double avgPrice = activites.stream()
                .filter(a -> a.getPrix() != null)
                .mapToDouble(a -> a.getPrix().doubleValue())
                .average().orElse(0);

        long categoriesCount = activites.stream()
                .map(Activite::getCategorie)
                .filter(c -> c != null && !c.isBlank())
                .distinct().count();

        int occupancy = totalPlaces > 0 ? (int) Math.round(((double) (totalPlaces - totalDispo) / totalPlaces) * 100) : 0;

        addStatBox(stats, "\uD83C\uDFAF", String.valueOf(totalActivites), "Activités", bold, regular, GOLD);
        addStatBox(stats, "\uD83D\uDCB0", String.format("%.1f DT", avgPrice), "Prix moyen", bold, regular, SUCCESS_GREEN);
        addStatBox(stats, "\uD83D\uDCCA", occupancy + "%", "Occupation", bold, regular, WARM_ORANGE);
        addStatBox(stats, "\uD83C\uDFAD", String.valueOf(categoriesCount), "Catégories", bold, regular, ACCENT_BLUE);

        doc.add(stats);
    }

    private static void addStatBox(Table table, String emoji, String value, String label,
                                    PdfFont bold, PdfFont regular, DeviceRgb color) {
        Cell cell = new Cell().setBorder(Border.NO_BORDER).setTextAlignment(TextAlignment.CENTER).setPadding(10);
        cell.add(new Paragraph(emoji).setFontSize(20).setTextAlignment(TextAlignment.CENTER));
        cell.add(new Paragraph(value).setFont(bold).setFontSize(16).setFontColor(color).setTextAlignment(TextAlignment.CENTER));
        cell.add(new Paragraph(label).setFont(regular).setFontSize(8).setFontColor(MUTED_TEXT).setTextAlignment(TextAlignment.CENTER));
        table.addCell(cell);
    }

    private static void addLocationSection(Document doc, Etablissement etab, PdfFont bold, PdfFont regular) {
        Table locationTable = new Table(UnitValue.createPercentArray(new float[]{60, 40})).useAllAvailableWidth();
        locationTable.setBackgroundColor(CARD_BG).setPadding(14);

        // Left: Map image
        Cell mapCell = new Cell().setBorder(Border.NO_BORDER);

        byte[] mapImage = downloadStaticMap(null, null,
                safe(etab.getAdresse()) + ", " + safe(etab.getVille()));
        if (mapImage != null) {
            try {
                ImageData imgData = ImageDataFactory.create(mapImage);
                com.itextpdf.layout.element.Image map = new com.itextpdf.layout.element.Image(imgData);
                map.setWidth(UnitValue.createPercentValue(95));
                map.setBorderRadius(new com.itextpdf.layout.properties.BorderRadius(8));
                mapCell.add(map);
            } catch (Exception ignored) {
                mapCell.add(new Paragraph("Carte non disponible").setFont(regular).setFontSize(9).setFontColor(MUTED_TEXT));
            }
        } else {
            mapCell.add(new Paragraph("Carte non disponible").setFont(regular).setFontSize(9).setFontColor(MUTED_TEXT));
        }
        locationTable.addCell(mapCell);

        // Right: QR Code + address
        Cell qrCell = new Cell().setBorder(Border.NO_BORDER).setTextAlignment(TextAlignment.CENTER).setPadding(8);

        String mapsUrl = buildMapsUrl(null, null,
                safe(etab.getAdresse()) + ", " + safe(etab.getVille()));

        qrCell.add(new Paragraph("Scannez pour\nouvrir Google Maps")
                .setFont(bold).setFontSize(9).setFontColor(GOLD)
                .setTextAlignment(TextAlignment.CENTER).setMarginBottom(8));

        byte[] qrBytes = generateQRCode(mapsUrl, 150);
        if (qrBytes != null) {
            try {
                ImageData qrData = ImageDataFactory.create(qrBytes);
                com.itextpdf.layout.element.Image qrImg = new com.itextpdf.layout.element.Image(qrData);
                qrImg.setWidth(120);
                qrImg.setHorizontalAlignment(HorizontalAlignment.CENTER);
                qrCell.add(qrImg);
            } catch (Exception ignored) {}
        }

        qrCell.add(new Paragraph(safe(etab.getAdresse()))
                .setFont(regular).setFontSize(8).setFontColor(LIGHT_TEXT)
                .setTextAlignment(TextAlignment.CENTER).setMarginTop(6));
        qrCell.add(new Paragraph(safe(etab.getVille()))
                .setFont(regular).setFontSize(8).setFontColor(MUTED_TEXT)
                .setTextAlignment(TextAlignment.CENTER));

        locationTable.addCell(qrCell);
        doc.add(locationTable);
    }

    // ===== ACTIVITÉ COMPONENTS =====

    private static void addActiviteHero(Document doc, Activite a, PdfFont bold, PdfFont regular) {
        Table hero = new Table(UnitValue.createPercentArray(new float[]{70, 30})).useAllAvailableWidth();
        hero.setBackgroundColor(CARD_BG).setPadding(16).setMarginTop(14);

        Cell left = new Cell().setBorder(Border.NO_BORDER);
        left.add(new Paragraph(safe(a.getNomActivite()))
                .setFont(bold).setFontSize(20).setFontColor(ColorConstants.WHITE));

        StringBuilder sub = new StringBuilder();
        if (!safe(a.getCategorie()).isBlank()) sub.append(safe(a.getCategorie()).toUpperCase());
        if (!safe(a.getNiveau()).isBlank()) {
            if (sub.length() > 0) sub.append("  •  ");
            sub.append(a.getNiveau());
        }
        if (a.getDuree() != null) {
            if (sub.length() > 0) sub.append("  •  ");
            sub.append(a.getDuree()).append(" min");
        }
        left.add(new Paragraph(sub.toString())
                .setFont(regular).setFontSize(11).setFontColor(GOLD).setMarginTop(4));

        // Status badge
        String statut = safe(a.getStatut());
        if (!statut.isBlank()) {
            DeviceRgb statusColor = statut.equalsIgnoreCase("disponible") ? SUCCESS_GREEN :
                    statut.equalsIgnoreCase("annulee") ? new DeviceRgb(244, 67, 54) : WARM_ORANGE;
            left.add(new Paragraph("\u25CF " + statut.toUpperCase())
                    .setFont(bold).setFontSize(9).setFontColor(statusColor).setMarginTop(6));
        }

        hero.addCell(left);

        Cell right = new Cell().setBorder(Border.NO_BORDER)
                .setVerticalAlignment(VerticalAlignment.MIDDLE)
                .setTextAlignment(TextAlignment.RIGHT);
        right.add(new Paragraph(getCategoryIcon(safe(a.getCategorie())))
                .setFontSize(36).setTextAlignment(TextAlignment.RIGHT));
        hero.addCell(right);

        doc.add(hero);
    }

    private static void addActiviteInfoTable(Document doc, Activite a, PdfFont bold, PdfFont regular) {
        Table table = new Table(UnitValue.createPercentArray(new float[]{35, 65})).useAllAvailableWidth();
        table.setBackgroundColor(CARD_BG).setPadding(12);

        addInfoRow(table, "Catégorie", safe(a.getCategorie()), bold, regular);
        addInfoRow(table, "Niveau", safe(a.getNiveau()), bold, regular);
        addInfoRow(table, "Durée", a.getDuree() != null ? a.getDuree() + " min" : "—", bold, regular);
        addInfoRow(table, "Statut", safe(a.getStatut()), bold, regular);
        addInfoRow(table, "Adresse départ", safe(a.getAdresseDepart()), bold, regular);
        addInfoRow(table, "Âge minimum", a.getAgeMin() != null ? a.getAgeMin() + " ans" : "—", bold, regular);

        if (a.getDateDebut() != null) addInfoRow(table, "Date début", DATE_FMT.format(a.getDateDebut()), bold, regular);
        if (a.getDateFin() != null) addInfoRow(table, "Date fin", DATE_FMT.format(a.getDateFin()), bold, regular);

        doc.add(table);
    }

    private static void addEquipmentCard(Document doc, Activite a, PdfFont bold, PdfFont regular) {
        Table table = new Table(UnitValue.createPercentArray(new float[]{50, 50})).useAllAvailableWidth();
        table.setBackgroundColor(CARD_BG).setPadding(14);

        Cell equipCell = new Cell().setBorder(Border.NO_BORDER).setPadding(8);
        equipCell.add(new Paragraph("\uD83C\uDFD2 Équipement inclus").setFont(bold).setFontSize(10).setFontColor(GOLD));
        equipCell.add(new Paragraph(safe(a.getEquipementInclus()).isBlank() ? "—" : a.getEquipementInclus())
                .setFont(regular).setFontSize(9).setFontColor(LIGHT_TEXT).setMarginTop(4));
        table.addCell(equipCell);

        Cell condCell = new Cell().setBorder(Border.NO_BORDER).setPadding(8);
        condCell.add(new Paragraph("\u26A0 Conditions d'annulation").setFont(bold).setFontSize(10).setFontColor(GOLD));
        condCell.add(new Paragraph(safe(a.getConditionsAnnulation()).isBlank() ? "—" : a.getConditionsAnnulation())
                .setFont(regular).setFontSize(9).setFontColor(LIGHT_TEXT).setMarginTop(4));
        table.addCell(condCell);

        doc.add(table);
    }

    private static void addPricingCard(Document doc, Activite a, PdfFont bold, PdfFont regular) {
        Table pricing = new Table(UnitValue.createPercentArray(new float[]{33, 33, 34})).useAllAvailableWidth();
        pricing.setBackgroundColor(CARD_BG).setPadding(14);

        String prix = a.getPrix() != null ? a.getPrix().stripTrailingZeros().toPlainString() + " " + safe(a.getDevise()) : "Gratuit";
        addStatBox(pricing, "\uD83D\uDCB0", prix, "Prix", bold, regular, GOLD);

        String places = a.getNbPlaces() != null ? String.valueOf(a.getNbPlaces()) : "—";
        addStatBox(pricing, "\uD83D\uDCBA", places, "Places totales", bold, regular, ACCENT_BLUE);

        String dispo = a.getPlacesDispo() != null ? String.valueOf(a.getPlacesDispo()) : "—";
        DeviceRgb dispoColor = a.getPlacesDispo() != null && a.getPlacesDispo() > 0 ? SUCCESS_GREEN : new DeviceRgb(244, 67, 54);
        addStatBox(pricing, "\u2705", dispo, "Places dispo", bold, regular, dispoColor);

        doc.add(pricing);
    }

    private static void addSingleActivityItinerary(Document doc, Activite act, PdfFont bold, PdfFont regular) {
        Table itinerary = new Table(UnitValue.createPercentArray(new float[]{15, 85})).useAllAvailableWidth();
        itinerary.setBackgroundColor(CARD_BG).setPadding(14);

        int duration = act.getDuree() != null ? act.getDuree() : 60;

        addItineraryStep(itinerary, "09:00", "\uD83D\uDEEC  Départ",
                "Rendez-vous au point de départ : " + (safe(act.getAdresseDepart()).isBlank() ? "à confirmer" : act.getAdresseDepart()),
                bold, regular, ACCENT_BLUE);

        addItineraryStep(itinerary, "09:15", "\uD83D\uDCCB  Briefing & Préparation",
                "Présentation de l'activité, consignes de sécurité, vérification de l'équipement",
                bold, regular, WARM_ORANGE);

        addItineraryStep(itinerary, "09:30", getCategoryIcon(safe(act.getCategorie())) + "  " + safe(act.getNomActivite()),
                "Durée estimée : " + duration + " min | Niveau : " + safe(act.getNiveau()),
                bold, regular, GOLD);

        int endMin = 30 + duration;
        int endHour = 9 + endMin / 60;
        endMin = endMin % 60;
        addItineraryStep(itinerary, String.format("%02d:%02d", endHour, endMin), "\u2615  Pause & Debriefing",
                "Retour au point de départ, remise de l'équipement",
                bold, regular, MUTED_TEXT);

        addItineraryStep(itinerary, String.format("%02d:%02d", endHour, endMin + 15), "\uD83C\uDF1F  Fin",
                "Fin de l'activité. N'oubliez pas de partager votre expérience !",
                bold, regular, SUCCESS_GREEN);

        doc.add(itinerary);
    }

    private static void addActivityLocationSection(Document doc, Activite act, PdfFont bold, PdfFont regular) {
        Table locationTable = new Table(UnitValue.createPercentArray(new float[]{50, 50})).useAllAvailableWidth();
        locationTable.setBackgroundColor(CARD_BG).setPadding(14);

        // Left: map
        Cell mapCell = new Cell().setBorder(Border.NO_BORDER);
        byte[] mapImage = downloadStaticMap(null, null, safe(act.getAdresseDepart()));
        if (mapImage != null) {
            try {
                ImageData imgData = ImageDataFactory.create(mapImage);
                com.itextpdf.layout.element.Image map = new com.itextpdf.layout.element.Image(imgData);
                map.setWidth(UnitValue.createPercentValue(95));
                mapCell.add(map);
            } catch (Exception ignored) {
                mapCell.add(new Paragraph("Carte non disponible").setFont(regular).setFontSize(9).setFontColor(MUTED_TEXT));
            }
        } else {
            mapCell.add(new Paragraph("Carte non disponible").setFont(regular).setFontSize(9).setFontColor(MUTED_TEXT));
        }
        locationTable.addCell(mapCell);

        // Right: QR
        Cell qrCell = new Cell().setBorder(Border.NO_BORDER).setTextAlignment(TextAlignment.CENTER).setPadding(8);
        String mapsUrl = buildMapsUrl(null, null, safe(act.getAdresseDepart()));

        qrCell.add(new Paragraph("Scannez pour\nouvrir Google Maps")
                .setFont(bold).setFontSize(9).setFontColor(GOLD)
                .setTextAlignment(TextAlignment.CENTER).setMarginBottom(8));

        byte[] qrBytes = generateQRCode(mapsUrl, 150);
        if (qrBytes != null) {
            try {
                ImageData qrData = ImageDataFactory.create(qrBytes);
                com.itextpdf.layout.element.Image qrImg = new com.itextpdf.layout.element.Image(qrData);
                qrImg.setWidth(120).setHorizontalAlignment(HorizontalAlignment.CENTER);
                qrCell.add(qrImg);
            } catch (Exception ignored) {}
        }

        qrCell.add(new Paragraph(safe(act.getAdresseDepart()))
                .setFont(regular).setFontSize(8).setFontColor(LIGHT_TEXT)
                .setTextAlignment(TextAlignment.CENTER).setMarginTop(6));

        locationTable.addCell(qrCell);
        doc.add(locationTable);
    }

    // ===================================================================
    //  UTILITAIRES
    // ===================================================================

    private static void addDescriptionCard(Document doc, String description, PdfFont regular) {
        Table card = new Table(1).useAllAvailableWidth();
        card.setBackgroundColor(CARD_BG).setPadding(14);

        Cell cell = new Cell().setBorder(Border.NO_BORDER);
        cell.add(new Paragraph(description)
                .setFont(regular).setFontSize(10).setFontColor(LIGHT_TEXT)
                .setFixedLeading(16));
        card.addCell(cell);
        doc.add(card);
    }

    private static void addInfoRow(Table table, String label, String value, PdfFont bold, PdfFont regular) {
        if (value == null || value.isBlank()) value = "—";

        Cell labelCell = new Cell()
                .add(new Paragraph(label.toUpperCase()).setFont(bold).setFontSize(8).setFontColor(GOLD))
                .setBorder(Border.NO_BORDER)
                .setBorderBottom(new SolidBorder(new DeviceRgb(50, 50, 65), 0.3f))
                .setPadding(6);
        table.addCell(labelCell);

        Cell valueCell = new Cell()
                .add(new Paragraph(value).setFont(regular).setFontSize(9).setFontColor(LIGHT_TEXT))
                .setBorder(Border.NO_BORDER)
                .setBorderBottom(new SolidBorder(new DeviceRgb(50, 50, 65), 0.3f))
                .setPadding(6);
        table.addCell(valueCell);
    }

    private static void addTableCell(Table table, String text, PdfFont regular) {
        table.addCell(new Cell()
                .add(new Paragraph(text).setFont(regular).setFontSize(8).setFontColor(LIGHT_TEXT))
                .setBackgroundColor(CARD_BG)
                .setBorder(new SolidBorder(new DeviceRgb(50, 50, 65), 0.3f))
                .setPadding(5));
    }

    private static void addFooter(Document doc, PdfFont regular) {
        doc.add(new Paragraph("\n"));
        Table footer = new Table(1).useAllAvailableWidth();
        footer.addCell(new Cell().setBackgroundColor(GOLD).setHeight(1).setBorder(Border.NO_BORDER));
        doc.add(footer);

        doc.add(new Paragraph("Premium Travel Agency — Document généré automatiquement")
                .setFont(regular).setFontSize(8).setFontColor(MUTED_TEXT)
                .setTextAlignment(TextAlignment.CENTER).setMarginTop(6));
        doc.add(new Paragraph("Ce document contient un QR Code interactif. Scannez-le pour accéder à la localisation.")
                .setFont(regular).setFontSize(7).setFontColor(MUTED_TEXT)
                .setTextAlignment(TextAlignment.CENTER));
    }

    // ===== QR CODE GENERATION =====
    private static byte[] generateQRCode(String text, int size) {
        try {
            QRCodeWriter writer = new QRCodeWriter();
            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            hints.put(EncodeHintType.MARGIN, 1);

            BitMatrix matrix = writer.encode(text, BarcodeFormat.QR_CODE, size, size, hints);
            BufferedImage image = MatrixToImageWriter.toBufferedImage(matrix);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "PNG", baos);
            return baos.toByteArray();
        } catch (Exception e) {
            System.err.println("[PdfExport] QR generation error: " + e.getMessage());
            return null;
        }
    }

    // ===== GOOGLE MAPS STATIC IMAGE =====
    private static byte[] downloadStaticMap(Double lat, Double lng, String fallbackAddress) {
        try {
            String center;
            if (lat != null && lng != null) {
                center = lat + "," + lng;
            } else if (fallbackAddress != null && !fallbackAddress.isBlank()) {
                center = URLEncoder.encode(fallbackAddress, StandardCharsets.UTF_8);
            } else {
                return null;
            }

            // OpenStreetMap static map via staticmap.openstreetmap.de (free, no API key)
            String urlStr;
            if (lat != null && lng != null) {
                urlStr = "https://staticmap.openstreetmap.de/staticmap.php?"
                        + "center=" + lat + "," + lng
                        + "&zoom=15&size=400x250&maptype=mapnik"
                        + "&markers=" + lat + "," + lng + ",red-pushpin";
            } else {
                // For address-only, we can't easily geocode, return null
                return null;
            }

            HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(8000);
            conn.setRequestProperty("User-Agent", "JavaFX-TravelApp/1.0");

            if (conn.getResponseCode() == 200) {
                try (InputStream is = conn.getInputStream()) {
                    return is.readAllBytes();
                }
            }
        } catch (Exception e) {
            System.err.println("[PdfExport] Map download error: " + e.getMessage());
        }
        return null;
    }

    private static String buildMapsUrl(Double lat, Double lng, String address) {
        try {
            // Prefer address text so Maps resolves the actual place name
            if (address != null && !address.isBlank() && !address.equals(", ")) {
                return "https://www.google.com/maps/search/?api=1&query="
                        + URLEncoder.encode(address, StandardCharsets.UTF_8);
            } else if (lat != null && lng != null) {
                return "https://www.google.com/maps/search/?api=1&query=" + lat + "," + lng;
            }
        } catch (Exception ignored) {}
        return "https://maps.google.com";
    }

    // ===== DB HELPER =====
    private static List<Activite> getActivitesByEtablissement(int idEtab) {
        try {
            ActiviteServices svc = new ActiviteServices();
            List<Activite> all = svc.afficher();
            List<Activite> result = new ArrayList<>();
            for (Activite a : all) {
                if (a.getIdEtablissement() != null && a.getIdEtablissement() == idEtab) {
                    result.add(a);
                }
            }
            return result;
        } catch (SQLException e) {
            System.err.println("[PdfExport] DB error: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    // ===== EMOJI HELPERS =====
    private static String getTypeEmoji(String type) {
        if (type == null) return "\uD83C\uDFE2";
        return switch (type.toLowerCase()) {
            case "hotel" -> "\uD83C\uDFE8";
            case "restaurant" -> "\uD83C\uDF7D";
            case "cafe" -> "\u2615";
            case "museum" -> "\uD83C\uDFDB";
            case "bar" -> "\uD83C\uDF7A";
            default -> "\uD83C\uDFE2";
        };
    }

    private static String getCategoryIcon(String categorie) {
        if (categorie == null) return "\uD83C\uDFAF";
        return switch (categorie.toLowerCase()) {
            case "sport" -> "\u26BD";
            case "culture" -> "\uD83C\uDFAD";
            case "loisir" -> "\uD83C\uDFA1";
            case "nature" -> "\uD83C\uDF3F";
            default -> "\uD83C\uDFAF";
        };
    }

    private static String safe(String s) { return s == null ? "" : s; }

    // ===================================================================
    //  EXPORT CATALOGUE — TOUS LES ÉTABLISSEMENTS
    // ===================================================================
    public static void exportAllEtablissements(List<Etablissement> etablissements, File outputFile) throws Exception {
        PdfWriter writer = new PdfWriter(outputFile);
        PdfDocument pdf = new PdfDocument(writer);
        Document doc = new Document(pdf, PageSize.A4);
        doc.setMargins(30, 35, 30, 35);

        PdfFont bold = PdfFontFactory.createFont("Helvetica-Bold");
        PdfFont regular = PdfFontFactory.createFont("Helvetica");

        addPageBackground(pdf);

        // ===== HEADER =====
        addPremiumHeader(doc, bold, regular, "CATALOGUE DES \u00c9TABLISSEMENTS");

        // ===== SUMMARY BAR =====
        Table summary = new Table(UnitValue.createPercentArray(new float[]{25, 25, 25, 25})).useAllAvailableWidth();
        summary.setBackgroundColor(CARD_BG).setPadding(10);

        long nbHotels = etablissements.stream().filter(e -> "hotel".equalsIgnoreCase(safe(e.getType()))).count();
        long nbRestos = etablissements.stream().filter(e -> "restaurant".equalsIgnoreCase(safe(e.getType()))).count();
        Set<String> villes = new HashSet<>();
        for (Etablissement e : etablissements) if (!safe(e.getVille()).isBlank()) villes.add(e.getVille());

        addStatBox(summary, "\uD83C\uDFE2", String.valueOf(etablissements.size()), "Total", bold, regular, GOLD);
        addStatBox(summary, "\uD83C\uDFE8", String.valueOf(nbHotels), "H\u00f4tels", bold, regular, ACCENT_BLUE);
        addStatBox(summary, "\uD83C\uDF7D", String.valueOf(nbRestos), "Restaurants", bold, regular, WARM_ORANGE);
        addStatBox(summary, "\uD83C\uDF0D", String.valueOf(villes.size()), "Villes", bold, regular, SUCCESS_GREEN);
        doc.add(summary);
        doc.add(new Paragraph("\n"));

        // ===== TABLE =====
        addSectionTitle(doc, bold, "\uD83D\uDCCB  LISTE COMPL\u00c8TE");

        Table table = new Table(UnitValue.createPercentArray(new float[]{5, 22, 16, 13, 14, 15, 15})).useAllAvailableWidth();
        table.setBackgroundColor(CARD_BG);

        String[] headers = {"#", "Nom", "Ville", "Type", "Gamme", "T\u00e9l\u00e9phone", "Email"};
        for (String h : headers) {
            table.addHeaderCell(new Cell()
                    .add(new Paragraph(h).setFont(bold).setFontSize(8).setFontColor(GOLD))
                    .setBackgroundColor(new DeviceRgb(45, 45, 60))
                    .setBorder(new SolidBorder(new DeviceRgb(60, 60, 80), 0.5f))
                    .setPadding(5));
        }

        int idx = 1;
        for (Etablissement e : etablissements) {
            addTableCell(table, String.valueOf(idx++), regular);
            addTableCell(table, safe(e.getNom()), regular);
            addTableCell(table, safe(e.getVille()), regular);
            addTableCell(table, safe(e.getType()), regular);
            addTableCell(table, safe(e.getGammePrix()), regular);
            addTableCell(table, safe(e.getTelephone()), regular);
            addTableCell(table, safe(e.getEmail()), regular);
        }
        doc.add(table);

        // ===== DETAIL CARDS =====
        doc.add(new Paragraph("\n"));
        addSectionTitle(doc, bold, "\uD83D\uDCC4  FICHES D\u00c9TAILL\u00c9ES");

        for (Etablissement e : etablissements) {
            Table card = new Table(1).useAllAvailableWidth();
            card.setBackgroundColor(CARD_BG).setPadding(12).setMarginBottom(10);

            Cell titleCell = new Cell().setBorder(Border.NO_BORDER);
            titleCell.add(new Paragraph(getTypeEmoji(e.getType()) + "  " + safe(e.getNom()))
                    .setFont(bold).setFontSize(12).setFontColor(GOLD));
            if (!safe(e.getAdresse()).isBlank() || !safe(e.getVille()).isBlank()) {
                titleCell.add(new Paragraph("\uD83D\uDCCD " + safe(e.getAdresse())
                        + (!safe(e.getVille()).isBlank() ? ", " + e.getVille() : ""))
                        .setFont(regular).setFontSize(8).setFontColor(MUTED_TEXT));
            }
            card.addCell(titleCell);

            // Info row
            StringBuilder info = new StringBuilder();
            if (!safe(e.getType()).isBlank()) info.append("Type: ").append(e.getType());
            if (!safe(e.getGammePrix()).isBlank()) {
                if (info.length() > 0) info.append("  \u2022  ");
                info.append("Gamme: ").append(e.getGammePrix());
            }
            if (!safe(e.getTelephone()).isBlank()) {
                if (info.length() > 0) info.append("  \u2022  ");
                info.append("\u260E ").append(e.getTelephone());
            }
            if (!safe(e.getEmail()).isBlank()) {
                if (info.length() > 0) info.append("  \u2022  ");
                info.append("\u2709 ").append(e.getEmail());
            }
            if (info.length() > 0) {
                Cell infoCell = new Cell().setBorder(Border.NO_BORDER);
                infoCell.add(new Paragraph(info.toString()).setFont(regular).setFontSize(8).setFontColor(LIGHT_TEXT));
                card.addCell(infoCell);
            }

            // Description
            if (!safe(e.getDescription()).isBlank()) {
                String desc = e.getDescription().length() > 200 ? e.getDescription().substring(0, 200) + "..." : e.getDescription();
                Cell descCell = new Cell().setBorder(Border.NO_BORDER);
                descCell.add(new Paragraph(desc).setFont(regular).setFontSize(7).setFontColor(MUTED_TEXT).setMarginTop(4));
                card.addCell(descCell);
            }

            doc.add(card);
        }

        addPageBackground(pdf);
        addFooter(doc, regular);
        doc.close();
    }

    // ===================================================================
    //  EXPORT CATALOGUE — TOUTES LES ACTIVITÉS
    // ===================================================================
    public static void exportAllActivites(List<Activite> activites, File outputFile) throws Exception {
        PdfWriter writer = new PdfWriter(outputFile);
        PdfDocument pdf = new PdfDocument(writer);
        Document doc = new Document(pdf, PageSize.A4);
        doc.setMargins(30, 35, 30, 35);

        PdfFont bold = PdfFontFactory.createFont("Helvetica-Bold");
        PdfFont regular = PdfFontFactory.createFont("Helvetica");

        addPageBackground(pdf);

        // ===== HEADER =====
        addPremiumHeader(doc, bold, regular, "CATALOGUE DES ACTIVIT\u00c9S");

        // ===== SUMMARY BAR =====
        Table summary = new Table(UnitValue.createPercentArray(new float[]{25, 25, 25, 25})).useAllAvailableWidth();
        summary.setBackgroundColor(CARD_BG).setPadding(10);

        double avgPrix = activites.stream()
                .filter(a -> a.getPrix() != null)
                .mapToDouble(a -> a.getPrix().doubleValue())
                .average().orElse(0);
        long nbSport = activites.stream().filter(a -> "Sport".equalsIgnoreCase(safe(a.getCategorie()))).count();
        long nbCulture = activites.stream().filter(a -> "Culture".equalsIgnoreCase(safe(a.getCategorie()))).count();

        addStatBox(summary, "\uD83C\uDFAF", String.valueOf(activites.size()), "Total", bold, regular, GOLD);
        addStatBox(summary, "\u26BD", String.valueOf(nbSport), "Sport", bold, regular, SUCCESS_GREEN);
        addStatBox(summary, "\uD83C\uDFAD", String.valueOf(nbCulture), "Culture", bold, regular, ACCENT_BLUE);
        addStatBox(summary, "\uD83D\uDCB0", String.format("%.0f DT", avgPrix), "Prix moy.", bold, regular, WARM_ORANGE);
        doc.add(summary);
        doc.add(new Paragraph("\n"));

        // ===== TABLE =====
        addSectionTitle(doc, bold, "\uD83D\uDCCB  LISTE COMPL\u00c8TE");

        Table table = new Table(UnitValue.createPercentArray(new float[]{5, 24, 14, 12, 12, 14, 19})).useAllAvailableWidth();
        table.setBackgroundColor(CARD_BG);

        String[] tableHeaders = {"#", "Nom", "Cat\u00e9gorie", "Niveau", "Dur\u00e9e", "Prix", "Adresse d\u00e9part"};
        for (String h : tableHeaders) {
            table.addHeaderCell(new Cell()
                    .add(new Paragraph(h).setFont(bold).setFontSize(8).setFontColor(GOLD))
                    .setBackgroundColor(new DeviceRgb(45, 45, 60))
                    .setBorder(new SolidBorder(new DeviceRgb(60, 60, 80), 0.5f))
                    .setPadding(5));
        }

        int num = 1;
        for (Activite a : activites) {
            addTableCell(table, String.valueOf(num++), regular);
            addTableCell(table, safe(a.getNomActivite()), regular);
            addTableCell(table, safe(a.getCategorie()), regular);
            addTableCell(table, safe(a.getNiveau()), regular);
            addTableCell(table, a.getDuree() != null ? a.getDuree() + " min" : "\u2014", regular);
            String prix = "\u2014";
            if (a.getPrix() != null) {
                prix = a.getPrix().stripTrailingZeros().toPlainString() + " " + safe(a.getDevise());
            }
            addTableCell(table, prix, regular);
            addTableCell(table, safe(a.getAdresseDepart()), regular);
        }
        doc.add(table);

        // ===== DETAIL CARDS =====
        doc.add(new Paragraph("\n"));
        addSectionTitle(doc, bold, "\uD83D\uDCC4  FICHES D\u00c9TAILL\u00c9ES");

        for (Activite a : activites) {
            Table card = new Table(1).useAllAvailableWidth();
            card.setBackgroundColor(CARD_BG).setPadding(12).setMarginBottom(10);

            Cell titleCell = new Cell().setBorder(Border.NO_BORDER);
            titleCell.add(new Paragraph(getCategoryIcon(a.getCategorie()) + "  " + safe(a.getNomActivite()))
                    .setFont(bold).setFontSize(12).setFontColor(GOLD));
            if (!safe(a.getAdresseDepart()).isBlank()) {
                titleCell.add(new Paragraph("\uD83D\uDCCD " + a.getAdresseDepart())
                        .setFont(regular).setFontSize(8).setFontColor(MUTED_TEXT));
            }
            card.addCell(titleCell);

            // Info row
            StringBuilder info = new StringBuilder();
            if (!safe(a.getCategorie()).isBlank()) info.append("Cat\u00e9gorie: ").append(a.getCategorie());
            if (!safe(a.getNiveau()).isBlank()) {
                if (info.length() > 0) info.append("  \u2022  ");
                info.append("Niveau: ").append(a.getNiveau());
            }
            if (a.getDuree() != null) {
                if (info.length() > 0) info.append("  \u2022  ");
                info.append("\u23F1 ").append(a.getDuree()).append(" min");
            }
            if (a.getPrix() != null) {
                if (info.length() > 0) info.append("  \u2022  ");
                info.append("\uD83D\uDCB0 ").append(a.getPrix().stripTrailingZeros().toPlainString())
                        .append(" ").append(safe(a.getDevise()));
            }
            if (a.getNbPlaces() != null) {
                if (info.length() > 0) info.append("  \u2022  ");
                info.append("\uD83D\uDC65 ").append(a.getNbPlaces()).append(" places");
            }
            if (info.length() > 0) {
                Cell infoCell = new Cell().setBorder(Border.NO_BORDER);
                infoCell.add(new Paragraph(info.toString()).setFont(regular).setFontSize(8).setFontColor(LIGHT_TEXT));
                card.addCell(infoCell);
            }

            // Description
            if (!safe(a.getDescription()).isBlank()) {
                String desc = a.getDescription().length() > 200 ? a.getDescription().substring(0, 200) + "..." : a.getDescription();
                Cell descCell = new Cell().setBorder(Border.NO_BORDER);
                descCell.add(new Paragraph(desc).setFont(regular).setFontSize(7).setFontColor(MUTED_TEXT).setMarginTop(4));
                card.addCell(descCell);
            }

            doc.add(card);
        }

        addPageBackground(pdf);
        addFooter(doc, regular);
        doc.close();
    }
}
