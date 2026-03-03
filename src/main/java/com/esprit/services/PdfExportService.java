package com.esprit.services;

import com.esprit.entities.Adresse;
import com.esprit.entities.LieuTouristique;
import com.esprit.entities.categorie;
import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.io.image.ImageData;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.draw.SolidLine;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.properties.HorizontalAlignment;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.layout.properties.VerticalAlignment;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Professional PDF report generator for tourist places.
 * Generates elegant reports with cover page, summary stats, and detailed place pages.
 */
public class PdfExportService {

    // Brand colors
    private static final DeviceRgb GOLD = new DeviceRgb(191, 162, 0);
    private static final DeviceRgb DARK_BG = new DeviceRgb(13, 15, 25);
    private static final DeviceRgb DARK_CARD = new DeviceRgb(18, 22, 38);
    private static final DeviceRgb LIGHT_TEXT = new DeviceRgb(220, 220, 220);
    private static final DeviceRgb MUTED_TEXT = new DeviceRgb(160, 160, 160);
    private static final DeviceRgb GREEN = new DeviceRgb(39, 174, 96);
    private static final DeviceRgb RED = new DeviceRgb(192, 57, 43);
    private static final DeviceRgb WHITE = new DeviceRgb(255, 255, 255);

    /**
     * Generate a full professional PDF report.
     *
     * @param file        output file
     * @param lieux       list of tourist places
     * @param categories  list of categories (for name lookups)
     * @param adresses    list of addresses (for coordinate lookups)
     */
    public void generateReport(File file, List<LieuTouristique> lieux,
                                List<categorie> categories, List<Adresse> adresses) throws Exception {

        Map<Integer, String> catNames = categories.stream()
                .collect(Collectors.toMap(categorie::getIdcategorie, categorie::getNomcategorie, (a, b) -> a));
        Map<Integer, Adresse> adresseMap = adresses.stream()
                .collect(Collectors.toMap(Adresse::getId_adresse, a -> a, (a, b) -> a));

        PdfWriter writer = new PdfWriter(file);
        PdfDocument pdfDoc = new PdfDocument(writer);
        Document doc = new Document(pdfDoc, PageSize.A4);
        doc.setMargins(40, 40, 50, 40);

        PdfFont fontBold = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
        PdfFont fontRegular = PdfFontFactory.createFont(StandardFonts.HELVETICA);
        PdfFont fontItalic = PdfFontFactory.createFont(StandardFonts.HELVETICA_OBLIQUE);

        // ======================== COVER PAGE ========================
        buildCoverPage(doc, fontBold, fontRegular, fontItalic, lieux.size(), categories.size());

        // ======================== SUMMARY PAGE ========================
        doc.add(new AreaBreak());
        buildSummaryPage(doc, fontBold, fontRegular, lieux, catNames);

        // ======================== DETAILED PLACE PAGES ========================
        int index = 1;
        for (LieuTouristique lieu : lieux) {
            doc.add(new AreaBreak());
            buildPlacePage(doc, fontBold, fontRegular, fontItalic, lieu, catNames, adresseMap, index);
            index++;
        }

        // ======================== FOOTER NOTE ========================
        doc.add(new AreaBreak());
        buildFooterPage(doc, fontBold, fontRegular, fontItalic, lieux.size());

        doc.close();
        System.out.println("✅ PDF report generated: " + file.getAbsolutePath());
    }

    // ==================== COVER PAGE ====================

    private void buildCoverPage(Document doc, PdfFont bold, PdfFont regular, PdfFont italic,
                                 int totalLieux, int totalCat) {
        // Top decoration line
        doc.add(new LineSeparator(new SolidLine(3)).setStrokeColor(GOLD).setMarginBottom(60));

        // Main title
        doc.add(new Paragraph("GESTION TOURISTIQUE")
                .setFont(bold).setFontSize(36).setFontColor(GOLD)
                .setTextAlignment(TextAlignment.CENTER).setMarginBottom(8));

        doc.add(new Paragraph("Rapport des Lieux Touristiques")
                .setFont(regular).setFontSize(18).setFontColor(MUTED_TEXT)
                .setTextAlignment(TextAlignment.CENTER).setMarginBottom(40));

        doc.add(new LineSeparator(new SolidLine(1)).setStrokeColor(MUTED_TEXT).setMarginBottom(40));

        // Stats box
        Table statsTable = new Table(UnitValue.createPercentArray(new float[]{1, 1}))
                .useAllAvailableWidth().setMarginBottom(40);

        statsTable.addCell(createStatCell(bold, regular, String.valueOf(totalLieux), "Lieux Touristiques"));
        statsTable.addCell(createStatCell(bold, regular, String.valueOf(totalCat), "Catégories"));
        doc.add(statsTable);

        // Date
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("dd MMMM yyyy"));
        doc.add(new Paragraph("Généré le " + today)
                .setFont(italic).setFontSize(12).setFontColor(MUTED_TEXT)
                .setTextAlignment(TextAlignment.CENTER).setMarginTop(80));

        doc.add(new Paragraph("Application de Gestion Touristique — Esprit")
                .setFont(regular).setFontSize(10).setFontColor(MUTED_TEXT)
                .setTextAlignment(TextAlignment.CENTER).setMarginTop(10));

        // Bottom decoration
        doc.add(new LineSeparator(new SolidLine(3)).setStrokeColor(GOLD).setMarginTop(60));
    }

    private Cell createStatCell(PdfFont bold, PdfFont regular, String number, String label) {
        Cell cell = new Cell().setBorder(Border.NO_BORDER).setPadding(20)
                .setTextAlignment(TextAlignment.CENTER);
        cell.add(new Paragraph(number).setFont(bold).setFontSize(42).setFontColor(GOLD).setMarginBottom(4));
        cell.add(new Paragraph(label).setFont(regular).setFontSize(12).setFontColor(MUTED_TEXT));
        return cell;
    }

    // ==================== SUMMARY PAGE ====================

    private void buildSummaryPage(Document doc, PdfFont bold, PdfFont regular,
                                   List<LieuTouristique> lieux, Map<Integer, String> catNames) {
        doc.add(new Paragraph("📊  Résumé Statistique")
                .setFont(bold).setFontSize(22).setFontColor(GOLD).setMarginBottom(20));

        doc.add(new LineSeparator(new SolidLine(2)).setStrokeColor(GOLD).setMarginBottom(20));

        // Summary stats
        double avgPrice = lieux.stream().mapToDouble(LieuTouristique::getPrix).average().orElse(0);
        double maxPrice = lieux.stream().mapToDouble(LieuTouristique::getPrix).max().orElse(0);
        double minPrice = lieux.stream().mapToDouble(LieuTouristique::getPrix).min().orElse(0);
        long active = lieux.stream().filter(l -> l.getStatut() == 1).count();
        long inactive = lieux.size() - active;
        long distinctCities = lieux.stream()
                .map(LieuTouristique::getVille)
                .filter(v -> v != null && !v.isEmpty())
                .map(String::toLowerCase).distinct().count();

        Table summaryTable = new Table(UnitValue.createPercentArray(new float[]{1, 1, 1}))
                .useAllAvailableWidth().setMarginBottom(25);
        summaryTable.addCell(createKpiCell(bold, regular, String.format("%.0f TND", avgPrice), "Prix Moyen"));
        summaryTable.addCell(createKpiCell(bold, regular, String.format("%.0f TND", maxPrice), "Prix Maximum"));
        summaryTable.addCell(createKpiCell(bold, regular, String.format("%.0f TND", minPrice), "Prix Minimum"));
        doc.add(summaryTable);

        Table summaryTable2 = new Table(UnitValue.createPercentArray(new float[]{1, 1, 1}))
                .useAllAvailableWidth().setMarginBottom(30);
        summaryTable2.addCell(createKpiCell(bold, regular, String.valueOf(active), "Disponibles"));
        summaryTable2.addCell(createKpiCell(bold, regular, String.valueOf(inactive), "Indisponibles"));
        summaryTable2.addCell(createKpiCell(bold, regular, String.valueOf(distinctCities), "Villes"));
        doc.add(summaryTable2);

        // Full list table
        doc.add(new Paragraph("📋  Liste Complète")
                .setFont(bold).setFontSize(16).setFontColor(GOLD).setMarginBottom(12));

        Table table = new Table(UnitValue.createPercentArray(new float[]{0.5f, 2.5f, 1.5f, 1.5f, 1, 1}))
                .useAllAvailableWidth();

        // Header
        String[] headers = {"#", "Nom", "Ville", "Catégorie", "Prix", "Statut"};
        for (String h : headers) {
            table.addHeaderCell(new Cell().add(new Paragraph(h).setFont(bold).setFontSize(9).setFontColor(WHITE))
                    .setBackgroundColor(GOLD).setPadding(6).setBorder(Border.NO_BORDER));
        }

        // Rows
        int i = 1;
        boolean alt = false;
        for (LieuTouristique l : lieux) {
            DeviceRgb rowBg = alt ? new DeviceRgb(245, 245, 245) : new DeviceRgb(255, 255, 255);
            DeviceRgb textColor = new DeviceRgb(40, 40, 40);

            table.addCell(cellWithStyle(String.valueOf(i), regular, 8, textColor, rowBg));
            table.addCell(cellWithStyle(l.getNom() != null ? l.getNom() : "-", bold, 8, textColor, rowBg));
            table.addCell(cellWithStyle(l.getVille() != null ? l.getVille() : "-", regular, 8, textColor, rowBg));
            table.addCell(cellWithStyle(catNames.getOrDefault(l.getId_categorie(), "-"), regular, 8, textColor, rowBg));
            table.addCell(cellWithStyle(String.format("%.0f TND", l.getPrix()), regular, 8, textColor, rowBg));

            String statut = l.getStatut() == 1 ? "✅ Actif" : "❌ Inactif";
            DeviceRgb statusColor = l.getStatut() == 1 ? GREEN : RED;
            table.addCell(cellWithStyle(statut, bold, 8, statusColor, rowBg));

            i++;
            alt = !alt;
        }

        doc.add(table);
    }

    private Cell createKpiCell(PdfFont bold, PdfFont regular, String value, String label) {
        Cell cell = new Cell().setBorder(new SolidBorder(GOLD, 1)).setPadding(14)
                .setTextAlignment(TextAlignment.CENTER).setBorderRadius(null);
        cell.add(new Paragraph(value).setFont(bold).setFontSize(20).setFontColor(GOLD).setMarginBottom(2));
        cell.add(new Paragraph(label).setFont(regular).setFontSize(9).setFontColor(MUTED_TEXT));
        return cell;
    }

    private Cell cellWithStyle(String text, PdfFont font, int size, DeviceRgb color, DeviceRgb bg) {
        return new Cell().add(new Paragraph(text).setFont(font).setFontSize(size).setFontColor(color))
                .setBackgroundColor(bg).setPadding(5).setBorder(Border.NO_BORDER)
                .setBorderBottom(new SolidBorder(new DeviceRgb(230, 230, 230), 0.5f));
    }

    // ==================== INDIVIDUAL PLACE PAGE ====================

    private void buildPlacePage(Document doc, PdfFont bold, PdfFont regular, PdfFont italic,
                                 LieuTouristique lieu, Map<Integer, String> catNames,
                                 Map<Integer, Adresse> adresseMap, int index) {

        // Header with number
        doc.add(new Paragraph("Lieu #" + index)
                .setFont(regular).setFontSize(10).setFontColor(MUTED_TEXT).setMarginBottom(4));

        doc.add(new Paragraph(lieu.getNom() != null ? lieu.getNom() : "Sans nom")
                .setFont(bold).setFontSize(24).setFontColor(GOLD).setMarginBottom(6));

        doc.add(new LineSeparator(new SolidLine(2)).setStrokeColor(GOLD).setMarginBottom(16));

        // Image + Details side by side
        Table layout = new Table(UnitValue.createPercentArray(new float[]{1.2f, 2}))
                .useAllAvailableWidth().setMarginBottom(20);

        // Left: Image
        Cell imageCell = new Cell().setBorder(Border.NO_BORDER).setPadding(4);
        boolean imageLoaded = false;
        if (lieu.getImage() != null && !lieu.getImage().isEmpty()) {
            try {
                ImageData imgData;
                String imagePath = lieu.getImage();
                if (imagePath.startsWith("http://") || imagePath.startsWith("https://")) {
                    imgData = ImageDataFactory.create(new URL(imagePath));
                } else {
                    File imgFile = new File(imagePath);
                    if (imgFile.exists()) {
                        imgData = ImageDataFactory.create(imgFile.getAbsolutePath());
                    } else {
                        imgData = null;
                    }
                }
                if (imgData != null) {
                    Image img = new Image(imgData).setMaxWidth(200).setMaxHeight(180);
                    img.setBorderRadius(null);
                    imageCell.add(img);
                    imageLoaded = true;
                }
            } catch (Exception e) {
                System.err.println("⚠ PDF: Could not load image for " + lieu.getNom() + ": " + e.getMessage());
            }
        }
        if (!imageLoaded) {
            imageCell.add(new Paragraph("🏛️")
                    .setFont(regular).setFontSize(48).setFontColor(MUTED_TEXT)
                    .setTextAlignment(TextAlignment.CENTER).setPaddingTop(40));
            imageCell.add(new Paragraph("Aucune image")
                    .setFont(italic).setFontSize(9).setFontColor(MUTED_TEXT)
                    .setTextAlignment(TextAlignment.CENTER));
        }
        layout.addCell(imageCell);

        // Right: Details
        Cell detailsCell = new Cell().setBorder(Border.NO_BORDER).setPaddingLeft(16);

        String catName = catNames.getOrDefault(lieu.getId_categorie(), "-");
        Adresse addr = adresseMap.get(lieu.getId_adresse());

        addDetailRow(detailsCell, bold, regular, "Catégorie", catName);
        addDetailRow(detailsCell, bold, regular, "Ville", lieu.getVille() != null ? lieu.getVille() : "-");
        addDetailRow(detailsCell, bold, regular, "Prix", String.format("%.2f TND", lieu.getPrix()));

        String statusText = lieu.getStatut() == 1 ? "✅ Disponible" : "❌ Indisponible";
        addDetailRow(detailsCell, bold, regular, "Statut", statusText);

        if (addr != null) {
            addDetailRow(detailsCell, bold, regular, "Adresse", addr.getRue() != null ? addr.getRue() : "-");
            addDetailRow(detailsCell, bold, regular, "Coordonnées",
                    String.format("%.4f, %.4f", addr.getLatitude(), addr.getLongitude()));
        }

        layout.addCell(detailsCell);
        doc.add(layout);

        // Description
        if (lieu.getDescription() != null && !lieu.getDescription().isEmpty()) {
            doc.add(new Paragraph("Description")
                    .setFont(bold).setFontSize(13).setFontColor(GOLD).setMarginBottom(6));

            doc.add(new Paragraph(lieu.getDescription())
                    .setFont(regular).setFontSize(10).setFontColor(new DeviceRgb(60, 60, 60))
                    .setMarginBottom(12)
                    .setPaddingLeft(8)
                    .setBorderLeft(new SolidBorder(GOLD, 2)));
        }

        // Mini map link
        if (addr != null && addr.getLatitude() != 0 && addr.getLongitude() != 0) {
            String mapUrl = String.format("https://www.google.com/maps?q=%.6f,%.6f",
                    addr.getLatitude(), addr.getLongitude());
            doc.add(new Paragraph("📍 Voir sur Google Maps: ")
                    .setFont(regular).setFontSize(9).setFontColor(MUTED_TEXT).setMarginTop(8)
                    .add(new Text(mapUrl).setFont(regular).setFontSize(9).setFontColor(GOLD)
                            .setUnderline()));
        }

        // Static map image via OpenStreetMap tile
        if (addr != null && addr.getLatitude() != 0 && addr.getLongitude() != 0) {
            try {
                int zoom = 14;
                double latRad = Math.toRadians(addr.getLatitude());
                int xtile = (int) Math.floor((addr.getLongitude() + 180.0) / 360.0 * (1 << zoom));
                int ytile = (int) Math.floor((1.0 - Math.log(Math.tan(latRad) + 1.0 / Math.cos(latRad)) / Math.PI) / 2.0 * (1 << zoom));
                String tileUrl = String.format("https://tile.openstreetmap.org/%d/%d/%d.png", zoom, xtile, ytile);
                java.net.URLConnection conn = new URL(tileUrl).openConnection();
                conn.setRequestProperty("User-Agent", "GestionTouristique/1.0");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);
                byte[] tileBytes = conn.getInputStream().readAllBytes();
                ImageData mapData = ImageDataFactory.create(tileBytes);
                Image mapImg = new Image(mapData).setMaxWidth(256).setMaxHeight(256).setMarginTop(10);
                doc.add(mapImg);
                doc.add(new Paragraph("Carte OpenStreetMap — zoom " + zoom)
                        .setFont(regular).setFontSize(7).setFontColor(MUTED_TEXT));
            } catch (Exception e) {
                // Static map loading is optional, skip silently
            }
        }
    }

    private void addDetailRow(Cell parent, PdfFont bold, PdfFont regular, String label, String value) {
        Paragraph p = new Paragraph()
                .add(new Text(label + ":  ").setFont(bold).setFontSize(10).setFontColor(GOLD))
                .add(new Text(value).setFont(regular).setFontSize(10).setFontColor(new DeviceRgb(60, 60, 60)))
                .setMarginBottom(6);
        parent.add(p);
    }

    // ==================== FOOTER PAGE ====================

    private void buildFooterPage(Document doc, PdfFont bold, PdfFont regular, PdfFont italic, int total) {
        doc.add(new LineSeparator(new SolidLine(2)).setStrokeColor(GOLD).setMarginBottom(40));

        doc.add(new Paragraph("Fin du Rapport")
                .setFont(bold).setFontSize(22).setFontColor(GOLD)
                .setTextAlignment(TextAlignment.CENTER).setMarginBottom(16));

        doc.add(new Paragraph("Ce rapport contient " + total + " lieux touristiques.")
                .setFont(regular).setFontSize(12).setFontColor(MUTED_TEXT)
                .setTextAlignment(TextAlignment.CENTER).setMarginBottom(30));

        doc.add(new Paragraph("Document généré automatiquement — " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))
                .setFont(italic).setFontSize(10).setFontColor(MUTED_TEXT)
                .setTextAlignment(TextAlignment.CENTER));

        doc.add(new Paragraph("Application de Gestion Touristique © Esprit 2026")
                .setFont(regular).setFontSize(9).setFontColor(MUTED_TEXT)
                .setTextAlignment(TextAlignment.CENTER).setMarginTop(8));

        doc.add(new LineSeparator(new SolidLine(2)).setStrokeColor(GOLD).setMarginTop(40));
    }

    // ==================== SINGLE PLACE PDF ====================

    /**
     * Generate a single-page PDF for one tourist place (e.g., from a card).
     */
    public void generateSinglePlaceReport(File file, LieuTouristique lieu,
                                            String categoryName, Adresse adresse) throws Exception {
        PdfWriter writer = new PdfWriter(file);
        PdfDocument pdfDoc = new PdfDocument(writer);
        Document doc = new Document(pdfDoc, PageSize.A4);
        doc.setMargins(40, 40, 50, 40);

        PdfFont bold = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
        PdfFont regular = PdfFontFactory.createFont(StandardFonts.HELVETICA);
        PdfFont italic = PdfFontFactory.createFont(StandardFonts.HELVETICA_OBLIQUE);

        Map<Integer, String> catNames = Map.of(lieu.getId_categorie(), categoryName != null ? categoryName : "-");
        Map<Integer, Adresse> adresseMap = adresse != null
                ? Map.of(lieu.getId_adresse(), adresse)
                : Map.of();

        // Title header
        doc.add(new LineSeparator(new SolidLine(3)).setStrokeColor(GOLD).setMarginBottom(20));

        doc.add(new Paragraph("FICHE LIEU TOURISTIQUE")
                .setFont(bold).setFontSize(14).setFontColor(MUTED_TEXT)
                .setTextAlignment(TextAlignment.CENTER).setMarginBottom(8));

        buildPlacePage(doc, bold, regular, italic, lieu, catNames, adresseMap, 1);

        doc.add(new LineSeparator(new SolidLine(3)).setStrokeColor(GOLD).setMarginTop(30));

        doc.add(new Paragraph("Gestion Touristique — " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd MMMM yyyy")))
                .setFont(italic).setFontSize(9).setFontColor(MUTED_TEXT)
                .setTextAlignment(TextAlignment.CENTER).setMarginTop(8));

        doc.close();
        System.out.println("✅ Single place PDF generated: " + file.getAbsolutePath());
    }
}
