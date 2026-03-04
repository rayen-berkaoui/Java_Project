package com.esprit.services;

import com.esprit.entities.Activite;
import com.esprit.entities.Etablissement;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

/**
 * Service for exporting establishments and activities to Excel (.xlsx) files.
 */
public class ExcelExportService {

    // ═══════════════════════════════════════════
    //  ESTABLISHMENTS
    // ═══════════════════════════════════════════

    public static void exportAllEtablissements(List<Etablissement> list, File file) throws IOException {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Établissements");

            // Header style
            CellStyle headerStyle = createHeaderStyle(wb);

            // Header row
            String[] headers = {"ID", "Nom", "Type", "Ville", "Adresse", "Téléphone",
                    "Email", "Gamme Prix", "Horaires", "Description"};
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Data rows
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

            // Auto-size columns
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
                // Cap max width at 50 chars
                if (sheet.getColumnWidth(i) > 50 * 256) {
                    sheet.setColumnWidth(i, 50 * 256);
                }
            }

            // Freeze header row
            sheet.createFreezePane(0, 1);

            try (FileOutputStream fos = new FileOutputStream(file)) {
                wb.write(fos);
            }
        }
    }

    // ═══════════════════════════════════════════
    //  ACTIVITIES
    // ═══════════════════════════════════════════

    public static void exportAllActivites(List<Activite> list, File file) throws IOException {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Activités");

            // Header style
            CellStyle headerStyle = createHeaderStyle(wb);

            // Header row
            String[] headers = {"ID", "Nom", "Catégorie", "Niveau", "Durée (min)",
                    "Prix", "Devise", "Statut", "Places", "Disponibles",
                    "Âge min", "Adresse départ", "Date début", "Date fin", "Description"};
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Data rows
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

            // Auto-size columns
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
                if (sheet.getColumnWidth(i) > 50 * 256) {
                    sheet.setColumnWidth(i, 50 * 256);
                }
            }

            // Freeze header row
            sheet.createFreezePane(0, 1);

            try (FileOutputStream fos = new FileOutputStream(file)) {
                wb.write(fos);
            }
        }
    }

    // ═══════════════════════════════════════════
    //  HELPERS
    // ═══════════════════════════════════════════

    private static CellStyle createHeaderStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 11);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.DARK_TEAL.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }
}
