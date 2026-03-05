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
import java.util.Map;
import java.util.stream.Collectors;

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
    // EXPORT TO PDF (Enhanced with Stats & Security Overview)
    // ========================
    public boolean exportToPDF(List<utilisateur> users, File file) {
        try {
            Document document = new Document(PageSize.A4.rotate());
            PdfWriter.getInstance(document, new FileOutputStream(file));
            document.open();

            // ── Colours ──
            Color gold       = new Color(255, 215, 0);
            Color darkBg     = new Color(40, 40, 40);
            Color greenClr   = new Color(81, 207, 102);
            Color redClr     = new Color(255, 107, 107);
            Color blueClr    = new Color(100, 181, 246);
            Color purpleClr  = new Color(206, 147, 216);
            Color grayText   = new Color(150, 150, 150);
            Color lightGray  = new Color(245, 245, 245);

            // ── Fonts ──
            Font titleFont  = new Font(Font.HELVETICA, 22, Font.BOLD, gold);
            Font subFont    = new Font(Font.HELVETICA, 10, Font.NORMAL, grayText);
            Font sectionFont= new Font(Font.HELVETICA, 13, Font.BOLD, gold);
            Font headerFont = new Font(Font.HELVETICA, 9, Font.BOLD, Color.WHITE);
            Font dataFont   = new Font(Font.HELVETICA, 8, Font.NORMAL, new Color(60, 60, 60));
            Font activeFont = new Font(Font.HELVETICA, 8, Font.BOLD, greenClr);
            Font blockedFont= new Font(Font.HELVETICA, 8, Font.BOLD, redClr);

            // ────── HEADER ──────
            Paragraph title = new Paragraph("TABAANI — User Report", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(4);
            document.add(title);

            Paragraph sub = new Paragraph(
                    "Generated " + java.time.LocalDateTime.now().toString().replace("T", " ").substring(0, 19),
                    subFont);
            sub.setAlignment(Element.ALIGN_CENTER);
            sub.setSpacingAfter(18);
            document.add(sub);

            // ────── STATS SUMMARY CARDS ──────
            int total   = users.size();
            long active  = users.stream().filter(u -> "ACTIF".equalsIgnoreCase(u.getStatut())).count();
            long blocked = users.stream().filter(u -> "BLOQUE".equalsIgnoreCase(u.getStatut())).count();
            long twoFA   = users.stream().filter(utilisateur::isTotpEnabled).count();
            long faceRec = users.stream().filter(u -> u.getFaceEncoding() != null && !u.getFaceEncoding().isEmpty()).count();

            double activeRate = total > 0 ? (active * 100.0 / total) : 0;
            double blockRate  = total > 0 ? (blocked * 100.0 / total) : 0;

            // Role distribution
            Map<Integer, Long> roleCounts = users.stream()
                    .collect(Collectors.groupingBy(utilisateur::getRoleId, Collectors.counting()));

            utilisateurServices us = new utilisateurServices();

            // 4-column stat card table
            PdfPTable statsTable = new PdfPTable(4);
            statsTable.setWidthPercentage(100);
            statsTable.setSpacingAfter(16);

            addStatCard(statsTable, "Total Users", String.valueOf(total), gold);
            addStatCard(statsTable, "Active (" + String.format("%.0f%%", activeRate) + ")", String.valueOf(active), greenClr);
            addStatCard(statsTable, "Blocked (" + String.format("%.0f%%", blockRate) + ")", String.valueOf(blocked), redClr);
            addStatCard(statsTable, "Roles", String.valueOf(roleCounts.size()), blueClr);

            document.add(statsTable);

            // ────── SECURITY OVERVIEW ──────
            Paragraph secTitle = new Paragraph("Security Overview", sectionFont);
            secTitle.setSpacingAfter(8);
            document.add(secTitle);

            PdfPTable secTable = new PdfPTable(4);
            secTable.setWidthPercentage(100);
            secTable.setSpacingAfter(16);

            addStatCard(secTable, "2FA Enabled", String.valueOf(twoFA), purpleClr);
            addStatCard(secTable, "Face Recognition", String.valueOf(faceRec), blueClr);
            addStatCard(secTable, "No 2FA", String.valueOf(total - twoFA), redClr);
            addStatCard(secTable, "Active Rate", String.format("%.1f%%", activeRate), greenClr);

            document.add(secTable);

            // ────── ROLE DISTRIBUTION ──────
            Paragraph roleTitle = new Paragraph("Role Distribution", sectionFont);
            roleTitle.setSpacingAfter(8);
            document.add(roleTitle);

            PdfPTable roleTable = new PdfPTable(3);
            roleTable.setWidthPercentage(60);
            roleTable.setHorizontalAlignment(Element.ALIGN_LEFT);
            roleTable.setSpacingAfter(18);
            roleTable.setWidths(new float[]{3f, 1.5f, 2f});

            Font roleHeader = new Font(Font.HELVETICA, 9, Font.BOLD, Color.WHITE);
            for (String h : new String[]{"Role", "Count", "Percentage"}) {
                PdfPCell c = new PdfPCell(new Phrase(h, roleHeader));
                c.setBackgroundColor(darkBg);
                c.setPadding(6);
                roleTable.addCell(c);
            }

            Color[] roleColors = {gold, greenClr, blueClr, purpleClr, redClr, new Color(255, 183, 77)};
            int idx = 0;
            for (Map.Entry<Integer, Long> entry : roleCounts.entrySet()) {
                String roleName = us.getRoleName(entry.getKey());
                long count = entry.getValue();
                double pct = total > 0 ? (count * 100.0 / total) : 0;
                Color color = roleColors[idx % roleColors.length];
                idx++;

                Font roleFont = new Font(Font.HELVETICA, 9, Font.BOLD, color);
                PdfPCell nameCell = new PdfPCell(new Phrase(roleName, roleFont));
                nameCell.setPadding(5);
                nameCell.setBackgroundColor(lightGray);
                roleTable.addCell(nameCell);

                PdfPCell countCell = new PdfPCell(new Phrase(String.valueOf(count), dataFont));
                countCell.setPadding(5);
                countCell.setBackgroundColor(lightGray);
                roleTable.addCell(countCell);

                PdfPCell pctCell = new PdfPCell(new Phrase(String.format("%.1f%%", pct), dataFont));
                pctCell.setPadding(5);
                pctCell.setBackgroundColor(lightGray);
                roleTable.addCell(pctCell);
            }

            document.add(roleTable);

            // ────── USERS TABLE ──────
            Paragraph tblTitle = new Paragraph("All Users", sectionFont);
            tblTitle.setSpacingAfter(8);
            document.add(tblTitle);

            PdfPTable table = new PdfPTable(8);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{1f, 2f, 2f, 3f, 1.5f, 1.2f, 2f, 2f});

            String[] headers = {"ID", "Nom", "Prenom", "Email", "Statut", "Role", "Telephone", "Date"};
            for (String h : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(h, headerFont));
                cell.setBackgroundColor(darkBg);
                cell.setPadding(8);
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                table.addCell(cell);
            }

            boolean alternate = false;
            for (utilisateur u : users) {
                Color bgColor = alternate ? lightGray : Color.WHITE;
                alternate = !alternate;

                addCell(table, String.valueOf(u.getId()), dataFont, bgColor);
                addCell(table, u.getNom() != null ? u.getNom() : "", dataFont, bgColor);
                addCell(table, u.getPrenom() != null ? u.getPrenom() : "", dataFont, bgColor);
                addCell(table, u.getEmail() != null ? u.getEmail() : "", dataFont, bgColor);

                String statut = u.getStatut() != null ? u.getStatut() : "";
                Font statusFont = "ACTIF".equalsIgnoreCase(statut) ? activeFont : blockedFont;
                addCell(table, statut, statusFont, bgColor);

                String roleName = us.getRoleName(u.getRoleId());
                addCell(table, roleName, dataFont, bgColor);
                addCell(table, String.valueOf(u.getNumTel()), dataFont, bgColor);
                addCell(table, u.getDateCreation() != null ? u.getDateCreation().toString() : "", dataFont, bgColor);
            }

            document.add(table);

            // ────── FOOTER ──────
            Paragraph footer = new Paragraph(
                    "\nTotal: " + total + " users  |  Active: " + active + "  |  Blocked: " + blocked
                    + "  |  2FA: " + twoFA + "  |  Face-ID: " + faceRec,
                    subFont);
            footer.setAlignment(Element.ALIGN_RIGHT);
            document.add(footer);

            document.close();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /** Helper: renders a stat-card-like cell inside a PdfPTable. */
    private void addStatCard(PdfPTable parent, String label, String value, Color accentColor) {
        Font valFont = new Font(Font.HELVETICA, 18, Font.BOLD, accentColor);
        Font lblFont = new Font(Font.HELVETICA, 9, Font.NORMAL, new Color(120, 120, 120));

        Paragraph p = new Paragraph();
        p.add(new Phrase(value + "\n", valFont));
        p.add(new Phrase(label, lblFont));

        PdfPCell card = new PdfPCell(p);
        card.setPaddingTop(10);
        card.setPaddingBottom(10);
        card.setPaddingLeft(12);
        card.setPaddingRight(12);
        card.setBackgroundColor(new Color(248, 248, 248));
        card.setBorderColor(new Color(230, 230, 230));
        card.setBorderWidth(1);
        parent.addCell(card);
    }

    private void addCell(PdfPTable table, String text, Font font, Color bgColor) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(bgColor);
        cell.setPadding(6);
        cell.setBorderColor(new Color(220, 220, 220));
        table.addCell(cell);
    }
}
