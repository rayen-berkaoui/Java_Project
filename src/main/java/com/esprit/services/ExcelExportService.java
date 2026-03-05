package com.esprit.services;

import com.esprit.entities.Adresse;
import com.esprit.entities.Activite;
import com.esprit.entities.Etablissement;
import com.esprit.entities.LieuTouristique;
import com.esprit.entities.categorie;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Professional Excel export service.
 * Generates .xlsx files with styled headers, data rows, conditional formatting, and summary sheet.
 */
public class ExcelExportService {

    // Gold brand color
    private static final byte[] GOLD_RGB = {(byte) 191, (byte) 162, 0};
    private static final byte[] DARK_GOLD_RGB = {(byte) 140, (byte) 120, 0};
    private static final byte[] LIGHT_GOLD_RGB = {(byte) 255, (byte) 245, (byte) 200};
    private static final byte[] WHITE_RGB = {(byte) 255, (byte) 255, (byte) 255};
    private static final byte[] GREEN_RGB = {(byte) 39, (byte) 174, (byte) 96};
    private static final byte[] RED_RGB = {(byte) 192, (byte) 57, (byte) 43};
    private static final byte[] LIGHT_GRAY_RGB = {(byte) 245, (byte) 245, (byte) 245};

    /**
     * Export all tourist places to a professional Excel file.
     */
    public void exportToExcel(File file, List<LieuTouristique> lieux,
                               List<categorie> categories, List<Adresse> adresses) throws Exception {

        Map<Integer, String> catNames = categories.stream()
                .collect(Collectors.toMap(categorie::getIdcategorie, categorie::getNomcategorie, (a, b) -> a));
        Map<Integer, Adresse> adresseMap = adresses.stream()
                .collect(Collectors.toMap(Adresse::getId_adresse, a -> a, (a, b) -> a));

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            // ===== SHEET 1: Lieux Touristiques =====
            XSSFSheet lieuxSheet = workbook.createSheet("Lieux Touristiques");
            buildLieuxSheet(workbook, lieuxSheet, lieux, catNames, adresseMap);

            // ===== SHEET 2: Catégories =====
            XSSFSheet catSheet = workbook.createSheet("Catégories");
            buildCategoriesSheet(workbook, catSheet, categories, lieux);

            // ===== SHEET 3: Adresses =====
            XSSFSheet addrSheet = workbook.createSheet("Adresses");
            buildAdressesSheet(workbook, addrSheet, adresses);

            // ===== SHEET 4: Statistiques =====
            XSSFSheet statsSheet = workbook.createSheet("Statistiques");
            buildStatsSheet(workbook, statsSheet, lieux, categories, adresses, catNames);

            // Set active sheet
            workbook.setActiveSheet(0);

            // Write file
            try (FileOutputStream fos = new FileOutputStream(file)) {
                workbook.write(fos);
            }
        }

        System.out.println("✅ Excel export generated: " + file.getAbsolutePath());
    }

    // ==================== SHEET: LIEUX TOURISTIQUES ====================

    private void buildLieuxSheet(XSSFWorkbook wb, XSSFSheet sheet,
                                  List<LieuTouristique> lieux,
                                  Map<Integer, String> catNames,
                                  Map<Integer, Adresse> adresseMap) {

        // Title row
        CellStyle titleStyle = createTitleStyle(wb);
        Row titleRow = sheet.createRow(0);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("🏛️  LIEUX TOURISTIQUES — Rapport Complet");
        titleCell.setCellStyle(titleStyle);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 9));
        titleRow.setHeightInPoints(36);

        // Subtitle
        CellStyle subStyle = createSubtitleStyle(wb);
        Row subRow = sheet.createRow(1);
        Cell subCell = subRow.createCell(0);
        subCell.setCellValue("Généré le " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) + " — " + lieux.size() + " lieux");
        subCell.setCellStyle(subStyle);
        sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, 9));

        // Header
        CellStyle headerStyle = createHeaderStyle(wb);
        String[] headers = {"#", "Nom", "Description", "Ville", "Catégorie", "Prix (TND)", "Statut", "Adresse", "Latitude", "Longitude"};
        Row headerRow = sheet.createRow(3);
        headerRow.setHeightInPoints(28);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // Data rows
        CellStyle evenStyle = createDataStyle(wb, false);
        CellStyle oddStyle = createDataStyle(wb, true);
        CellStyle priceStyle = createPriceStyle(wb);
        CellStyle activeStyle = createStatusStyle(wb, true);
        CellStyle inactiveStyle = createStatusStyle(wb, false);

        int rowNum = 4;
        int idx = 1;
        for (LieuTouristique l : lieux) {
            Row row = sheet.createRow(rowNum);
            row.setHeightInPoints(22);
            CellStyle rowStyle = (rowNum % 2 == 0) ? evenStyle : oddStyle;

            createStyledCell(row, 0, String.valueOf(idx), rowStyle);
            createStyledCell(row, 1, l.getNom() != null ? l.getNom() : "-", rowStyle);
            String desc = l.getDescription() != null ? l.getDescription() : "-";
            if (desc.length() > 100) desc = desc.substring(0, 97) + "...";
            createStyledCell(row, 2, desc, rowStyle);
            createStyledCell(row, 3, l.getVille() != null ? l.getVille() : "-", rowStyle);
            createStyledCell(row, 4, catNames.getOrDefault(l.getId_categorie(), "-"), rowStyle);

            Cell priceCell = row.createCell(5);
            priceCell.setCellValue(l.getPrix());
            priceCell.setCellStyle(priceStyle);

            Cell statusCell = row.createCell(6);
            statusCell.setCellValue(l.getStatut() == 1 ? "Disponible" : "Indisponible");
            statusCell.setCellStyle(l.getStatut() == 1 ? activeStyle : inactiveStyle);

            Adresse addr = adresseMap.get(l.getId_adresse());
            createStyledCell(row, 7, addr != null && addr.getRue() != null ? addr.getRue() : "-", rowStyle);
            if (addr != null) {
                Cell latCell = row.createCell(8);
                latCell.setCellValue(addr.getLatitude());
                latCell.setCellStyle(rowStyle);
                Cell lonCell = row.createCell(9);
                lonCell.setCellValue(addr.getLongitude());
                lonCell.setCellStyle(rowStyle);
            } else {
                createStyledCell(row, 8, "-", rowStyle);
                createStyledCell(row, 9, "-", rowStyle);
            }

            rowNum++;
            idx++;
        }

        // Auto-size columns
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
            // Set minimum widths for readability
            if (sheet.getColumnWidth(i) < 3000) sheet.setColumnWidth(i, 3000);
        }
        sheet.setColumnWidth(1, 8000); // Nom
        sheet.setColumnWidth(2, 12000); // Description
        sheet.setColumnWidth(7, 8000); // Adresse

        // Freeze header
        sheet.createFreezePane(0, 4);
        sheet.setAutoFilter(new CellRangeAddress(3, 3 + lieux.size(), 0, headers.length - 1));
    }

    // ==================== SHEET: CATEGORIES ====================

    private void buildCategoriesSheet(XSSFWorkbook wb, XSSFSheet sheet,
                                       List<categorie> categories, List<LieuTouristique> lieux) {

        CellStyle titleStyle = createTitleStyle(wb);
        Row titleRow = sheet.createRow(0);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("📂  CATÉGORIES");
        titleCell.setCellStyle(titleStyle);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 4));
        titleRow.setHeightInPoints(36);

        CellStyle headerStyle = createHeaderStyle(wb);
        String[] headers = {"#", "Nom", "Description", "Date Création", "Nb Lieux"};
        Row headerRow = sheet.createRow(2);
        headerRow.setHeightInPoints(28);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        CellStyle evenStyle = createDataStyle(wb, false);
        CellStyle oddStyle = createDataStyle(wb, true);

        Map<Integer, Long> catCount = lieux.stream()
                .collect(Collectors.groupingBy(LieuTouristique::getId_categorie, Collectors.counting()));

        int rowNum = 3;
        int idx = 1;
        for (categorie c : categories) {
            Row row = sheet.createRow(rowNum);
            CellStyle style = (rowNum % 2 == 0) ? evenStyle : oddStyle;

            createStyledCell(row, 0, String.valueOf(idx), style);
            createStyledCell(row, 1, c.getNomcategorie() != null ? c.getNomcategorie() : "-", style);
            createStyledCell(row, 2, c.getDescription() != null ? c.getDescription() : "-", style);
            createStyledCell(row, 3, c.getDateCreation() != null ? c.getDateCreation().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "-", style);

            Cell countCell = row.createCell(4);
            countCell.setCellValue(catCount.getOrDefault(c.getIdcategorie(), 0L));
            countCell.setCellStyle(style);

            rowNum++;
            idx++;
        }

        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
            if (sheet.getColumnWidth(i) < 3000) sheet.setColumnWidth(i, 3000);
        }
        sheet.setColumnWidth(1, 8000);
        sheet.setColumnWidth(2, 12000);
        sheet.createFreezePane(0, 3);
    }

    // ==================== SHEET: ADRESSES ====================

    private void buildAdressesSheet(XSSFWorkbook wb, XSSFSheet sheet, List<Adresse> adresses) {

        CellStyle titleStyle = createTitleStyle(wb);
        Row titleRow = sheet.createRow(0);
        titleRow.createCell(0).setCellValue("📍  ADRESSES");
        titleRow.getCell(0).setCellStyle(titleStyle);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 4));
        titleRow.setHeightInPoints(36);

        CellStyle headerStyle = createHeaderStyle(wb);
        String[] headers = {"#", "Rue", "Ville", "Latitude", "Longitude"};
        Row headerRow = sheet.createRow(2);
        headerRow.setHeightInPoints(28);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        CellStyle evenStyle = createDataStyle(wb, false);
        CellStyle oddStyle = createDataStyle(wb, true);

        int rowNum = 3;
        int idx = 1;
        for (Adresse a : adresses) {
            Row row = sheet.createRow(rowNum);
            CellStyle style = (rowNum % 2 == 0) ? evenStyle : oddStyle;

            createStyledCell(row, 0, String.valueOf(idx), style);
            createStyledCell(row, 1, a.getRue() != null ? a.getRue() : "-", style);
            createStyledCell(row, 2, a.getVille() != null ? a.getVille() : "-", style);

            Cell latCell = row.createCell(3);
            latCell.setCellValue(a.getLatitude());
            latCell.setCellStyle(style);

            Cell lonCell = row.createCell(4);
            lonCell.setCellValue(a.getLongitude());
            lonCell.setCellStyle(style);

            rowNum++;
            idx++;
        }

        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
            if (sheet.getColumnWidth(i) < 3000) sheet.setColumnWidth(i, 3000);
        }
        sheet.setColumnWidth(1, 10000);
        sheet.createFreezePane(0, 3);
    }

    // ==================== SHEET: STATISTIQUES ====================

    private void buildStatsSheet(XSSFWorkbook wb, XSSFSheet sheet,
                                  List<LieuTouristique> lieux, List<categorie> categories,
                                  List<Adresse> adresses, Map<Integer, String> catNames) {

        CellStyle titleStyle = createTitleStyle(wb);
        Row titleRow = sheet.createRow(0);
        titleRow.createCell(0).setCellValue("📊  STATISTIQUES");
        titleRow.getCell(0).setCellStyle(titleStyle);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 2));
        titleRow.setHeightInPoints(36);

        CellStyle labelStyle = createKpiLabelStyle(wb);
        CellStyle valueStyle = createKpiValueStyle(wb);

        int row = 2;

        // General stats
        row = addStatRow(sheet, row, "Total Lieux", String.valueOf(lieux.size()), labelStyle, valueStyle);
        row = addStatRow(sheet, row, "Total Catégories", String.valueOf(categories.size()), labelStyle, valueStyle);
        row = addStatRow(sheet, row, "Total Adresses", String.valueOf(adresses.size()), labelStyle, valueStyle);

        long cities = lieux.stream().map(LieuTouristique::getVille)
                .filter(Objects::nonNull).map(String::toLowerCase).distinct().count();
        row = addStatRow(sheet, row, "Villes distinctes", String.valueOf(cities), labelStyle, valueStyle);

        row++; // blank row

        // Price stats
        double avg = lieux.stream().mapToDouble(LieuTouristique::getPrix).average().orElse(0);
        double max = lieux.stream().mapToDouble(LieuTouristique::getPrix).max().orElse(0);
        double min = lieux.stream().mapToDouble(LieuTouristique::getPrix).min().orElse(0);
        row = addStatRow(sheet, row, "Prix moyen", String.format("%.2f TND", avg), labelStyle, valueStyle);
        row = addStatRow(sheet, row, "Prix maximum", String.format("%.2f TND", max), labelStyle, valueStyle);
        row = addStatRow(sheet, row, "Prix minimum", String.format("%.2f TND", min), labelStyle, valueStyle);

        row++; // blank row

        // Status
        long active = lieux.stream().filter(l -> l.getStatut() == 1).count();
        long inactive = lieux.size() - active;
        row = addStatRow(sheet, row, "Lieux Disponibles", String.valueOf(active), labelStyle, valueStyle);
        row = addStatRow(sheet, row, "Lieux Indisponibles", String.valueOf(inactive), labelStyle, valueStyle);

        if (!lieux.isEmpty()) {
            double rate = (double) active / lieux.size() * 100;
            row = addStatRow(sheet, row, "Taux d'activité", String.format("%.1f%%", rate), labelStyle, valueStyle);
        }

        row += 2;

        // Top 5 by price
        CellStyle headerStyle = createHeaderStyle(wb);
        Row topTitle = sheet.createRow(row);
        topTitle.createCell(0).setCellValue("🏆 Top 5 Lieux (par prix)");
        topTitle.getCell(0).setCellStyle(titleStyle);
        sheet.addMergedRegion(new CellRangeAddress(row, row, 0, 2));
        row++;

        Row topHeader = sheet.createRow(row);
        String[] th = {"Nom", "Ville", "Prix (TND)"};
        for (int i = 0; i < th.length; i++) {
            topHeader.createCell(i).setCellValue(th[i]);
            topHeader.getCell(i).setCellStyle(headerStyle);
        }
        row++;

        CellStyle evenStyle = createDataStyle(wb, false);
        List<LieuTouristique> top5 = lieux.stream()
                .sorted((a1, b1) -> Double.compare(b1.getPrix(), a1.getPrix()))
                .limit(5)
                .collect(Collectors.toList());
        for (LieuTouristique l : top5) {
            Row r = sheet.createRow(row);
            createStyledCell(r, 0, l.getNom() != null ? l.getNom() : "-", evenStyle);
            createStyledCell(r, 1, l.getVille() != null ? l.getVille() : "-", evenStyle);
            Cell pc = r.createCell(2);
            pc.setCellValue(l.getPrix());
            pc.setCellStyle(evenStyle);
            row++;
        }

        sheet.setColumnWidth(0, 10000);
        sheet.setColumnWidth(1, 8000);
        sheet.setColumnWidth(2, 5000);
    }

    private int addStatRow(XSSFSheet sheet, int rowNum, String label, String value,
                            CellStyle labelStyle, CellStyle valueStyle) {
        Row row = sheet.createRow(rowNum);
        Cell labelCell = row.createCell(0);
        labelCell.setCellValue(label);
        labelCell.setCellStyle(labelStyle);
        Cell valueCell = row.createCell(1);
        valueCell.setCellValue(value);
        valueCell.setCellStyle(valueStyle);
        return rowNum + 1;
    }

    // ==================== STYLE FACTORIES ====================

    private CellStyle createTitleStyle(XSSFWorkbook wb) {
        CellStyle style = wb.createCellStyle();
        XSSFFont font = wb.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 16);
        font.setColor(new XSSFColor(GOLD_RGB, null));
        style.setFont(font);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    private CellStyle createSubtitleStyle(XSSFWorkbook wb) {
        CellStyle style = wb.createCellStyle();
        XSSFFont font = wb.createFont();
        font.setFontHeightInPoints((short) 10);
        font.setItalic(true);
        font.setColor(new XSSFColor(new byte[]{(byte) 120, (byte) 120, (byte) 120}, null));
        style.setFont(font);
        return style;
    }

    private CellStyle createHeaderStyle(XSSFWorkbook wb) {
        CellStyle style = wb.createCellStyle();
        XSSFFont font = wb.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 10);
        font.setColor(new XSSFColor(WHITE_RGB, null));
        style.setFont(font);
        style.setFillForegroundColor(new XSSFColor(GOLD_RGB, null));
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setBorderBottom(BorderStyle.THIN);
        return style;
    }

    private CellStyle createDataStyle(XSSFWorkbook wb, boolean alternate) {
        CellStyle style = wb.createCellStyle();
        XSSFFont font = wb.createFont();
        font.setFontHeightInPoints((short) 9);
        style.setFont(font);
        if (alternate) {
            style.setFillForegroundColor(new XSSFColor(LIGHT_GRAY_RGB, null));
            style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        }
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setWrapText(true);
        return style;
    }

    private CellStyle createPriceStyle(XSSFWorkbook wb) {
        CellStyle style = wb.createCellStyle();
        XSSFFont font = wb.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 9);
        font.setColor(new XSSFColor(DARK_GOLD_RGB, null));
        style.setFont(font);
        DataFormat fmt = wb.createDataFormat();
        style.setDataFormat(fmt.getFormat("#,##0.00 \"TND\""));
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    private CellStyle createStatusStyle(XSSFWorkbook wb, boolean active) {
        CellStyle style = wb.createCellStyle();
        XSSFFont font = wb.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 9);
        font.setColor(new XSSFColor(active ? GREEN_RGB : RED_RGB, null));
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        byte[] bgColor = active ? new byte[]{(byte) 232, (byte) 255, (byte) 232} : new byte[]{(byte) 255, (byte) 232, (byte) 232};
        style.setFillForegroundColor(new XSSFColor(bgColor, null));
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }

    private CellStyle createKpiLabelStyle(XSSFWorkbook wb) {
        CellStyle style = wb.createCellStyle();
        XSSFFont font = wb.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 11);
        font.setColor(new XSSFColor(GOLD_RGB, null));
        style.setFont(font);
        return style;
    }

    private CellStyle createKpiValueStyle(XSSFWorkbook wb) {
        CellStyle style = wb.createCellStyle();
        XSSFFont font = wb.createFont();
        font.setFontHeightInPoints((short) 11);
        style.setFont(font);
        return style;
    }

    private void createStyledCell(Row row, int col, String value, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }

    // ==================== ETABLISSEMENT / ACTIVITE STATIC EXPORTS ====================

    public static void exportAllEtablissements(java.util.ArrayList<Etablissement> list, File file) throws IOException {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Établissements");
            CellStyle headerStyle = createStaticHeaderStyle(wb);

            String[] headers = {"ID", "Nom", "Type", "Ville", "Adresse", "Téléphone",
                    "Email", "Gamme Prix", "Horaires", "Description"};
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            CellStyle wrapStyle = wb.createCellStyle();
            wrapStyle.setWrapText(true);
            wrapStyle.setVerticalAlignment(VerticalAlignment.TOP);

            int rowIdx = 1;
            for (Etablissement e : list) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(e.getIdEtablissement());
                row.createCell(1).setCellValue(safe(e.getNom()));
                row.createCell(2).setCellValue(safe(e.getType()));
                row.createCell(3).setCellValue(safe(e.getVille()));
                row.createCell(4).setCellValue(safe(e.getAdresse()));
                row.createCell(5).setCellValue(safe(e.getTelephone()));
                row.createCell(6).setCellValue(safe(e.getEmail()));
                row.createCell(7).setCellValue(safe(e.getGammePrix()));
                row.createCell(8).setCellValue(safe(e.getHoraires()));
                Cell descCell = row.createCell(9);
                descCell.setCellValue(safe(e.getDescription()));
                descCell.setCellStyle(wrapStyle);
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
                if (sheet.getColumnWidth(i) > 50 * 256) sheet.setColumnWidth(i, 50 * 256);
            }
            sheet.createFreezePane(0, 1);

            try (FileOutputStream fos = new FileOutputStream(file)) {
                wb.write(fos);
            }
        }
    }

    public static void exportAllActivites(java.util.ArrayList<Activite> list, File file) throws IOException {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Activités");
            CellStyle headerStyle = createStaticHeaderStyle(wb);

            String[] headers = {"ID", "Nom", "Catégorie", "Niveau", "Durée (min)",
                    "Prix", "Devise", "Statut", "Places", "Disponibles",
                    "Âge min", "Adresse départ", "Date début", "Date fin", "Description"};
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            CellStyle wrapStyle = wb.createCellStyle();
            wrapStyle.setWrapText(true);
            wrapStyle.setVerticalAlignment(VerticalAlignment.TOP);

            int rowIdx = 1;
            for (Activite a : list) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(a.getIdActivite());
                row.createCell(1).setCellValue(safe(a.getNomActivite()));
                row.createCell(2).setCellValue(safe(a.getCategorie()));
                row.createCell(3).setCellValue(safe(a.getNiveau()));
                row.createCell(4).setCellValue(a.getDuree() != null ? a.getDuree() : 0);
                row.createCell(5).setCellValue(a.getPrix() != null ? a.getPrix().doubleValue() : 0);
                row.createCell(6).setCellValue(safe(a.getDevise()));
                row.createCell(7).setCellValue(safe(a.getStatut()));
                row.createCell(8).setCellValue(a.getNbPlaces() != null ? a.getNbPlaces() : 0);
                row.createCell(9).setCellValue(a.getPlacesDispo() != null ? a.getPlacesDispo() : 0);
                row.createCell(10).setCellValue(a.getAgeMin() != null ? a.getAgeMin() : 0);
                row.createCell(11).setCellValue(safe(a.getAdresseDepart()));
                row.createCell(12).setCellValue(a.getDateDebut() != null ? a.getDateDebut().toString() : "");
                row.createCell(13).setCellValue(a.getDateFin() != null ? a.getDateFin().toString() : "");
                Cell descCell = row.createCell(14);
                descCell.setCellValue(safe(a.getDescription()));
                descCell.setCellStyle(wrapStyle);
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
                if (sheet.getColumnWidth(i) > 50 * 256) sheet.setColumnWidth(i, 50 * 256);
            }
            sheet.createFreezePane(0, 1);

            try (FileOutputStream fos = new FileOutputStream(file)) {
                wb.write(fos);
            }
        }
    }

    private static CellStyle createStaticHeaderStyle(XSSFWorkbook wb) {
        CellStyle style = wb.createCellStyle();
        XSSFFont font = wb.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 11);
        font.setColor(new XSSFColor(new byte[]{(byte) 255, (byte) 255, (byte) 255}, null));
        style.setFont(font);
        style.setFillForegroundColor(new XSSFColor(new byte[]{(byte) 45, (byte) 45, (byte) 60}, null));
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    private static String safe(String s) {
        return s != null ? s : "";
    }
}
