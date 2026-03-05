package com.esprit.services;

import com.esprit.entities.utilisateur;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.Div;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Text;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.HorizontalAlignment;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.layout.properties.VerticalAlignment;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PiePlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.category.StandardBarPainter;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DefaultPieDataset;

import java.awt.Color;
import java.awt.Font;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Professional PDF Report Generator with embedded charts.
 * Uses iText 7 for PDF layout and JFreeChart for chart generation.
 * Includes:
 * - Header with branding
 * - Summary statistics cards
 * - Embedded Pie Chart (Status distribution)
 * - Embedded Bar Chart (Role distribution)
 * - Formatted user table with alternating row colors
 * - Exporter information
 * - Footer with page numbers
 */
public class PdfReportService {

    // Brand colors
    private static final DeviceRgb GOLD = new DeviceRgb(255, 215, 0);
    private static final DeviceRgb DARK_BG = new DeviceRgb(17, 17, 17);
    private static final DeviceRgb DARK_HEADER = new DeviceRgb(40, 40, 40);
    private static final DeviceRgb LIGHT_ROW = new DeviceRgb(250, 250, 250);
    private static final DeviceRgb WHITE = new DeviceRgb(255, 255, 255);
    private static final DeviceRgb GREEN = new DeviceRgb(81, 207, 102);
    private static final DeviceRgb RED = new DeviceRgb(255, 107, 107);
    private static final DeviceRgb BLUE = new DeviceRgb(100, 181, 246);
    private static final DeviceRgb GRAY_TEXT = new DeviceRgb(150, 150, 150);
    private static final DeviceRgb LIGHT_GRAY = new DeviceRgb(220, 220, 220);

    private final utilisateurServices userService;
    private final roleServices roleService;

    public PdfReportService() {
        userService = new utilisateurServices();
        roleService = new roleServices();
    }

    // =====================================================
    // ✅ GENERATE PROFESSIONAL PDF REPORT
    // =====================================================
    public boolean generateReport(List<utilisateur> users, File file, String exporterName, int exporterId) {
        try {
            PdfWriter writer = new PdfWriter(file);
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = new Document(pdfDoc, PageSize.A4.rotate());
            document.setMargins(30, 40, 40, 40);

            PdfFont regularFont = PdfFontFactory.createFont("Helvetica");
            PdfFont boldFont = PdfFontFactory.createFont("Helvetica-Bold");

            // ═══════ 1. HEADER ═══════
            addHeader(document, boldFont, regularFont, exporterName, exporterId);

            // ═══════ 2. SUMMARY STATISTICS ═══════
            addSummaryStats(document, users, boldFont, regularFont);

            // ═══════ 3. CHARTS ═══════
            addCharts(document, users);

            // ═══════ 4. USER TABLE ═══════
            addUserTable(document, users, boldFont, regularFont);

            // ═══════ 5. FOOTER ═══════
            addFooter(document, regularFont, exporterName);

            document.close();
            System.out.println("✅ Professional PDF report generated: " + file.getAbsolutePath());
            return true;

        } catch (Exception e) {
            System.out.println("❌ Failed to generate PDF report: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    // =====================================================
    // HEADER SECTION
    // =====================================================
    private void addHeader(Document document, PdfFont boldFont, PdfFont regularFont, 
                          String exporterName, int exporterId) {
        // Title bar
        Table headerTable = new Table(UnitValue.createPercentArray(new float[]{3, 2}))
                .useAllAvailableWidth()
                .setMarginBottom(5);

        // Left: Title
        Paragraph title = new Paragraph("TABAANI")
                .setFont(boldFont)
                .setFontSize(28)
                .setFontColor(GOLD)
                .setMarginBottom(0);

        Paragraph subtitle = new Paragraph("Rapport de gestion des utilisateurs")
                .setFont(regularFont)
                .setFontSize(12)
                .setFontColor(GRAY_TEXT)
                .setMarginTop(0);

        Cell titleCell = new Cell().add(title).add(subtitle)
                .setBorder(Border.NO_BORDER)
                .setVerticalAlignment(VerticalAlignment.MIDDLE);
        headerTable.addCell(titleCell);

        // Right: Export info
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
        Paragraph exportInfo = new Paragraph()
                .add(new Text("Date d'export: ").setFont(regularFont).setFontSize(10).setFontColor(GRAY_TEXT))
                .add(new Text(dateStr).setFont(boldFont).setFontSize(10).setFontColor(new DeviceRgb(80, 80, 80)))
                .add("\n")
                .add(new Text("Exporté par: ").setFont(regularFont).setFontSize(10).setFontColor(GRAY_TEXT))
                .add(new Text(exporterName + " (ID #" + exporterId + ")").setFont(boldFont).setFontSize(10).setFontColor(GOLD))
                .setTextAlignment(TextAlignment.RIGHT);

        Cell infoCell = new Cell().add(exportInfo)
                .setBorder(Border.NO_BORDER)
                .setVerticalAlignment(VerticalAlignment.MIDDLE);
        headerTable.addCell(infoCell);

        document.add(headerTable);

        // Gold divider line
        Table divider = new Table(1).useAllAvailableWidth()
                .setMarginBottom(20);
        Cell dividerCell = new Cell()
                .setHeight(2)
                .setBackgroundColor(GOLD)
                .setBorder(Border.NO_BORDER);
        divider.addCell(dividerCell);
        document.add(divider);
    }

    // =====================================================
    // SUMMARY STATISTICS CARDS
    // =====================================================
    private void addSummaryStats(Document document, List<utilisateur> users, 
                                PdfFont boldFont, PdfFont regularFont) {
        int total = users.size();
        int active = (int) users.stream().filter(u -> "ACTIF".equalsIgnoreCase(u.getStatut())).count();
        int blocked = (int) users.stream().filter(u -> "BLOQUE".equalsIgnoreCase(u.getStatut())).count();

        // Count unique roles
        Set<Integer> roleIds = new HashSet<>();
        for (utilisateur u : users) roleIds.add(u.getRoleId());
        int rolesCount = roleIds.size();

        Table statsTable = new Table(UnitValue.createPercentArray(new float[]{1, 1, 1, 1}))
                .useAllAvailableWidth()
                .setMarginBottom(20);

        statsTable.addCell(createStatCard("Total Utilisateurs", String.valueOf(total), GOLD, boldFont, regularFont));
        statsTable.addCell(createStatCard("Actifs", String.valueOf(active), GREEN, boldFont, regularFont));
        statsTable.addCell(createStatCard("Bloqués", String.valueOf(blocked), RED, boldFont, regularFont));
        statsTable.addCell(createStatCard("Rôles", String.valueOf(rolesCount), BLUE, boldFont, regularFont));

        document.add(statsTable);
    }

    private Cell createStatCard(String label, String value, DeviceRgb accentColor, 
                               PdfFont boldFont, PdfFont regularFont) {
        Paragraph valuePara = new Paragraph(value)
                .setFont(boldFont)
                .setFontSize(24)
                .setFontColor(accentColor)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(2);

        Paragraph labelPara = new Paragraph(label)
                .setFont(regularFont)
                .setFontSize(10)
                .setFontColor(GRAY_TEXT)
                .setTextAlignment(TextAlignment.CENTER);

        Cell cell = new Cell()
                .add(valuePara)
                .add(labelPara)
                .setBackgroundColor(new DeviceRgb(248, 248, 248))
                .setBorder(new SolidBorder(LIGHT_GRAY, 0.5f))
                .setPadding(12)
                .setVerticalAlignment(VerticalAlignment.MIDDLE);

        return cell;
    }

    // =====================================================
    // CHARTS SECTION (Pie + Bar)
    // =====================================================
    private void addCharts(Document document, List<utilisateur> users) {
        try {
            Table chartTable = new Table(UnitValue.createPercentArray(new float[]{1, 1}))
                    .useAllAvailableWidth()
                    .setMarginBottom(20);

            // Pie Chart: Status distribution
            byte[] pieChartBytes = generatePieChart(users);
            if (pieChartBytes != null) {
                Image pieImage = new Image(ImageDataFactory.create(pieChartBytes));
                pieImage.setWidth(UnitValue.createPercentValue(95));
                Cell pieCell = new Cell()
                        .add(pieImage)
                        .setBorder(new SolidBorder(LIGHT_GRAY, 0.5f))
                        .setPadding(8)
                        .setVerticalAlignment(VerticalAlignment.MIDDLE);
                chartTable.addCell(pieCell);
            }

            // Bar Chart: Role distribution
            byte[] barChartBytes = generateBarChart(users);
            if (barChartBytes != null) {
                Image barImage = new Image(ImageDataFactory.create(barChartBytes));
                barImage.setWidth(UnitValue.createPercentValue(95));
                Cell barCell = new Cell()
                        .add(barImage)
                        .setBorder(new SolidBorder(LIGHT_GRAY, 0.5f))
                        .setPadding(8)
                        .setVerticalAlignment(VerticalAlignment.MIDDLE);
                chartTable.addCell(barCell);
            }

            document.add(chartTable);

        } catch (Exception e) {
            System.out.println("⚠️ Chart generation failed: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private byte[] generatePieChart(List<utilisateur> users) {
        try {
            int active = (int) users.stream().filter(u -> "ACTIF".equalsIgnoreCase(u.getStatut())).count();
            int blocked = (int) users.stream().filter(u -> "BLOQUE".equalsIgnoreCase(u.getStatut())).count();
            int other = users.size() - active - blocked;

            DefaultPieDataset dataset = new DefaultPieDataset();
            dataset.setValue("Actifs (" + active + ")", active);
            dataset.setValue("Bloqués (" + blocked + ")", blocked);
            if (other > 0) dataset.setValue("Autre (" + other + ")", other);

            JFreeChart chart = ChartFactory.createPieChart(
                "Distribution par Statut", dataset, true, true, false
            );

            // Style
            chart.setBackgroundPaint(Color.WHITE);
            chart.getTitle().setFont(new Font("Helvetica", Font.BOLD, 14));
            chart.getTitle().setPaint(new Color(60, 60, 60));

            PiePlot plot = (PiePlot) chart.getPlot();
            plot.setBackgroundPaint(Color.WHITE);
            plot.setOutlineVisible(false);
            plot.setShadowPaint(null);
            plot.setLabelFont(new Font("Helvetica", Font.PLAIN, 11));
            plot.setSectionPaint("Actifs (" + active + ")", new Color(81, 207, 102));
            plot.setSectionPaint("Bloqués (" + blocked + ")", new Color(255, 107, 107));
            if (other > 0) plot.setSectionPaint("Autre (" + other + ")", new Color(255, 183, 77));

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ChartUtils.writeChartAsPNG(baos, chart, 380, 250);
            return baos.toByteArray();

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private byte[] generateBarChart(List<utilisateur> users) {
        try {
            // Count users per role
            Map<Integer, Integer> roleCounts = new LinkedHashMap<>();
            for (utilisateur u : users) {
                roleCounts.merge(u.getRoleId(), 1, Integer::sum);
            }

            DefaultCategoryDataset dataset = new DefaultCategoryDataset();
            for (Map.Entry<Integer, Integer> entry : roleCounts.entrySet()) {
                String roleName = userService.getRoleName(entry.getKey());
                dataset.addValue(entry.getValue(), "Utilisateurs", roleName);
            }

            JFreeChart chart = ChartFactory.createBarChart(
                "Distribution par Rôle", "Rôle", "Nombre",
                dataset, PlotOrientation.VERTICAL, false, true, false
            );

            // Style
            chart.setBackgroundPaint(Color.WHITE);
            chart.getTitle().setFont(new Font("Helvetica", Font.BOLD, 14));
            chart.getTitle().setPaint(new Color(60, 60, 60));

            CategoryPlot plot = chart.getCategoryPlot();
            plot.setBackgroundPaint(Color.WHITE);
            plot.setRangeGridlinePaint(new Color(220, 220, 220));
            plot.setOutlineVisible(false);

            BarRenderer renderer = (BarRenderer) plot.getRenderer();
            renderer.setBarPainter(new StandardBarPainter());
            renderer.setSeriesPaint(0, new Color(255, 215, 0));
            renderer.setShadowVisible(false);

            plot.getDomainAxis().setTickLabelFont(new Font("Helvetica", Font.PLAIN, 11));
            plot.getRangeAxis().setTickLabelFont(new Font("Helvetica", Font.PLAIN, 11));

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ChartUtils.writeChartAsPNG(baos, chart, 380, 250);
            return baos.toByteArray();

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // =====================================================
    // USER TABLE
    // =====================================================
    private void addUserTable(Document document, List<utilisateur> users,
                             PdfFont boldFont, PdfFont regularFont) {
        // Section title
        Paragraph tableTitle = new Paragraph("Liste des Utilisateurs")
                .setFont(boldFont)
                .setFontSize(14)
                .setFontColor(new DeviceRgb(60, 60, 60))
                .setMarginBottom(10);
        document.add(tableTitle);

        // Table
        float[] columnWidths = {1f, 2.2f, 2.2f, 3.5f, 1.5f, 1.5f, 2f, 2f};
        Table table = new Table(UnitValue.createPercentArray(columnWidths))
                .useAllAvailableWidth()
                .setFontSize(9);

        // Header row
        String[] headers = {"ID", "Nom", "Prénom", "Email", "Statut", "Rôle", "Téléphone", "Date Création"};
        for (String h : headers) {
            Cell headerCell = new Cell()
                    .add(new Paragraph(h).setFont(boldFont).setFontColor(WHITE).setFontSize(9))
                    .setBackgroundColor(DARK_HEADER)
                    .setPadding(8)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setBorder(new SolidBorder(new DeviceRgb(50, 50, 50), 0.5f));
            table.addHeaderCell(headerCell);
        }

        // Data rows with alternating colors
        boolean alternate = false;
        for (utilisateur u : users) {
            DeviceRgb bgColor = alternate ? LIGHT_ROW : WHITE;
            alternate = !alternate;

            String status = u.getStatut() != null ? u.getStatut() : "";
            DeviceRgb statusColor = "ACTIF".equalsIgnoreCase(status) ? GREEN : RED;
            String roleName = userService.getRoleName(u.getRoleId());

            addDataCell(table, String.valueOf(u.getId()), regularFont, bgColor, null);
            addDataCell(table, u.getNom() != null ? u.getNom() : "", regularFont, bgColor, null);
            addDataCell(table, u.getPrenom() != null ? u.getPrenom() : "", regularFont, bgColor, null);
            addDataCell(table, u.getEmail() != null ? u.getEmail() : "", regularFont, bgColor, null);
            addDataCell(table, status, boldFont, bgColor, statusColor);
            addDataCell(table, roleName, regularFont, bgColor, BLUE);
            addDataCell(table, String.valueOf(u.getNumTel()), regularFont, bgColor, null);
            addDataCell(table, u.getDateCreation() != null ? u.getDateCreation().toString() : "-", regularFont, bgColor, null);
        }

        document.add(table);

        // Total count
        Paragraph total = new Paragraph("Total: " + users.size() + " utilisateur(s)")
                .setFont(regularFont)
                .setFontSize(10)
                .setFontColor(GRAY_TEXT)
                .setTextAlignment(TextAlignment.RIGHT)
                .setMarginTop(8);
        document.add(total);
    }

    private void addDataCell(Table table, String text, PdfFont font, DeviceRgb bgColor, DeviceRgb textColor) {
        Paragraph p = new Paragraph(text).setFont(font).setFontSize(8.5f);
        if (textColor != null) {
            p.setFontColor(textColor);
        } else {
            p.setFontColor(new DeviceRgb(60, 60, 60));
        }

        Cell cell = new Cell()
                .add(p)
                .setBackgroundColor(bgColor)
                .setPadding(6)
                .setBorder(new SolidBorder(LIGHT_GRAY, 0.3f));
        table.addCell(cell);
    }

    // =====================================================
    // FOOTER
    // =====================================================
    private void addFooter(Document document, PdfFont regularFont, String exporterName) {
        document.add(new Paragraph("\n"));

        // Divider
        Table divider = new Table(1).useAllAvailableWidth();
        Cell dividerCell = new Cell()
                .setHeight(1)
                .setBackgroundColor(LIGHT_GRAY)
                .setBorder(Border.NO_BORDER);
        divider.addCell(dividerCell);
        document.add(divider);

        // Footer text
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm"));
        Paragraph footer = new Paragraph(
            "© 2026 Tabaani — Rapport généré le " + dateStr + " par " + exporterName +
            " | Document confidentiel"
        )
                .setFont(regularFont)
                .setFontSize(8)
                .setFontColor(GRAY_TEXT)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginTop(8);
        document.add(footer);
    }
}
