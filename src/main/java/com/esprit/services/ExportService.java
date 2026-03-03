package com.esprit.services;

import com.esprit.entities.utilisateur;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Font;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.opencsv.CSVWriter;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.awt.Color;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.util.List;

public class ExportService {

    // ========================
    // EXPORT TO CSV
    // ========================
    public boolean exportToCSV(List<utilisateur> users, File file) {
        try (CSVWriter writer = new CSVWriter(new FileWriter(file))) {
            // Header
            String[] header = {"ID", "Nom", "Prenom", "Email", "Statut", "Role ID", "Telephone", "Date Creation"};
            writer.writeNext(header);

            // Data
            for (utilisateur u : users) {
                String[] row = {
                    String.valueOf(u.getId()),
                    u.getNom() != null ? u.getNom() : "",
                    u.getPrenom() != null ? u.getPrenom() : "",
                    u.getEmail() != null ? u.getEmail() : "",
                    u.getStatut() != null ? u.getStatut() : "",
                    String.valueOf(u.getRoleId()),
                    String.valueOf(u.getNumTel()),
                    u.getDateCreation() != null ? u.getDateCreation().toString() : ""
                };
                writer.writeNext(row);
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // ========================
    // EXPORT TO EXCEL
    // ========================
    public boolean exportToExcel(List<utilisateur> users, File file) {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Utilisateurs");

            // Header style
            CellStyle headerStyle = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setFontHeightInPoints((short) 12);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.GOLD.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);

            // Data style
            CellStyle dataStyle = workbook.createCellStyle();
            dataStyle.setBorderBottom(BorderStyle.THIN);
            dataStyle.setBorderTop(BorderStyle.THIN);
            dataStyle.setBorderLeft(BorderStyle.THIN);
            dataStyle.setBorderRight(BorderStyle.THIN);

            // Header row
            String[] headers = {"ID", "Nom", "Prenom", "Email", "Statut", "Role ID", "Telephone", "Date Creation"};
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Data rows
            int rowIdx = 1;
            for (utilisateur u : users) {
                Row row = sheet.createRow(rowIdx++);
                Cell c0 = row.createCell(0); c0.setCellValue(u.getId()); c0.setCellStyle(dataStyle);
                Cell c1 = row.createCell(1); c1.setCellValue(u.getNom() != null ? u.getNom() : ""); c1.setCellStyle(dataStyle);
                Cell c2 = row.createCell(2); c2.setCellValue(u.getPrenom() != null ? u.getPrenom() : ""); c2.setCellStyle(dataStyle);
                Cell c3 = row.createCell(3); c3.setCellValue(u.getEmail() != null ? u.getEmail() : ""); c3.setCellStyle(dataStyle);
                Cell c4 = row.createCell(4); c4.setCellValue(u.getStatut() != null ? u.getStatut() : ""); c4.setCellStyle(dataStyle);
                Cell c5 = row.createCell(5); c5.setCellValue(u.getRoleId()); c5.setCellStyle(dataStyle);
                Cell c6 = row.createCell(6); c6.setCellValue(u.getNumTel()); c6.setCellStyle(dataStyle);
                Cell c7 = row.createCell(7); c7.setCellValue(u.getDateCreation() != null ? u.getDateCreation().toString() : ""); c7.setCellStyle(dataStyle);
            }

            // Auto-size columns
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            try (FileOutputStream fos = new FileOutputStream(file)) {
                workbook.write(fos);
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // ========================
    // EXPORT TO PDF
    // ========================
    public boolean exportToPDF(List<utilisateur> users, File file) {
        try {
            Document document = new Document(PageSize.A4.rotate());
            PdfWriter.getInstance(document, new FileOutputStream(file));
            document.open();

            // Title
            Font titleFont = new Font(Font.HELVETICA, 20, Font.BOLD, new Color(255, 215, 0));
            Paragraph title = new Paragraph("TABAANI - Liste des Utilisateurs", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(20);
            document.add(title);

            // Subtitle
            Font subFont = new Font(Font.HELVETICA, 10, Font.NORMAL, new Color(150, 150, 150));
            Paragraph sub = new Paragraph("Exporte le " + java.time.LocalDateTime.now().toString().replace("T", " ").substring(0, 19), subFont);
            sub.setAlignment(Element.ALIGN_CENTER);
            sub.setSpacingAfter(15);
            document.add(sub);

            // Table
            PdfPTable table = new PdfPTable(8);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{1f, 2f, 2f, 3f, 1.5f, 1.2f, 2f, 2f});

            // Header cells
            Font headerFont = new Font(Font.HELVETICA, 9, Font.BOLD, Color.WHITE);
            String[] headers = {"ID", "Nom", "Prenom", "Email", "Statut", "Role", "Telephone", "Date"};
            for (String h : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(h, headerFont));
                cell.setBackgroundColor(new Color(40, 40, 40));
                cell.setPadding(8);
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                table.addCell(cell);
            }

            // Data cells
            Font dataFont = new Font(Font.HELVETICA, 8, Font.NORMAL, new Color(60, 60, 60));
            Font activeFont = new Font(Font.HELVETICA, 8, Font.BOLD, new Color(81, 207, 102));
            Font blockedFont = new Font(Font.HELVETICA, 8, Font.BOLD, new Color(255, 107, 107));

            boolean alternate = false;
            for (utilisateur u : users) {
                Color bgColor = alternate ? new Color(245, 245, 245) : Color.WHITE;
                alternate = !alternate;

                addCell(table, String.valueOf(u.getId()), dataFont, bgColor);
                addCell(table, u.getNom() != null ? u.getNom() : "", dataFont, bgColor);
                addCell(table, u.getPrenom() != null ? u.getPrenom() : "", dataFont, bgColor);
                addCell(table, u.getEmail() != null ? u.getEmail() : "", dataFont, bgColor);

                String statut = u.getStatut() != null ? u.getStatut() : "";
                Font statusFont = "ACTIF".equalsIgnoreCase(statut) ? activeFont : blockedFont;
                addCell(table, statut, statusFont, bgColor);

                addCell(table, String.valueOf(u.getRoleId()), dataFont, bgColor);
                addCell(table, String.valueOf(u.getNumTel()), dataFont, bgColor);
                addCell(table, u.getDateCreation() != null ? u.getDateCreation().toString() : "", dataFont, bgColor);
            }

            document.add(table);

            // Footer
            Paragraph footer = new Paragraph("\nTotal: " + users.size() + " utilisateurs", subFont);
            footer.setAlignment(Element.ALIGN_RIGHT);
            document.add(footer);

            document.close();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private void addCell(PdfPTable table, String text, Font font, Color bgColor) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(bgColor);
        cell.setPadding(6);
        cell.setBorderColor(new Color(220, 220, 220));
        table.addCell(cell);
    }
}
