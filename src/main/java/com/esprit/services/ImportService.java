package com.esprit.services;

import com.esprit.entities.utilisateur;
import com.opencsv.CSVReader;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ImportService {

    private final utilisateurServices userService = new utilisateurServices();

    // ========================
    // IMPORT FROM CSV
    // ========================
    public ImportResult importFromCSV(File file) {
        ImportResult result = new ImportResult();
        try (CSVReader reader = new CSVReader(new FileReader(file))) {
            String[] line;
            boolean isHeader = true;

            while ((line = reader.readNext()) != null) {
                if (isHeader) {
                    isHeader = false;
                    continue; // skip header row
                }
                result.totalRows++;
                try {
                    if (line.length < 4) {
                        result.errors.add("Ligne " + (result.totalRows + 1) + ": pas assez de colonnes");
                        result.failedRows++;
                        continue;
                    }

                    String nom = line.length > 0 ? line[0].trim() : "";
                    String prenom = line.length > 1 ? line[1].trim() : "";
                    String email = line.length > 2 ? line[2].trim() : "";
                    String password = line.length > 3 ? line[3].trim() : "default123";
                    int numTel = 0;
                    if (line.length > 4 && !line[4].trim().isEmpty()) {
                        try { numTel = Integer.parseInt(line[4].trim()); } catch (NumberFormatException ignored) {}
                    }
                    int roleId = 1; // default TOURISTE
                    if (line.length > 5 && !line[5].trim().isEmpty()) {
                        try { roleId = Integer.parseInt(line[5].trim()); } catch (NumberFormatException ignored) {}
                    }

                    if (nom.isEmpty() || email.isEmpty()) {
                        result.errors.add("Ligne " + (result.totalRows + 1) + ": nom ou email vide");
                        result.failedRows++;
                        continue;
                    }

                    if (userService.emailExists(email)) {
                        result.errors.add("Ligne " + (result.totalRows + 1) + ": email deja existant (" + email + ")");
                        result.failedRows++;
                        continue;
                    }

                    boolean ok = userService.registerUser(nom, prenom, email, password, numTel, null, roleId);
                    if (ok) {
                        result.successRows++;
                    } else {
                        result.errors.add("Ligne " + (result.totalRows + 1) + ": erreur insertion DB");
                        result.failedRows++;
                    }
                } catch (Exception e) {
                    result.errors.add("Ligne " + (result.totalRows + 1) + ": " + e.getMessage());
                    result.failedRows++;
                }
            }
        } catch (Exception e) {
            result.errors.add("Erreur lecture fichier: " + e.getMessage());
            e.printStackTrace();
        }
        return result;
    }

    // ========================
    // IMPORT FROM EXCEL
    // ========================
    public ImportResult importFromExcel(File file) {
        ImportResult result = new ImportResult();
        try (FileInputStream fis = new FileInputStream(file);
             Workbook workbook = new XSSFWorkbook(fis)) {

            Sheet sheet = workbook.getSheetAt(0);
            boolean isHeader = true;

            for (Row row : sheet) {
                if (isHeader) {
                    isHeader = false;
                    continue; // skip header
                }
                result.totalRows++;
                try {
                    String nom = getCellString(row, 0);
                    String prenom = getCellString(row, 1);
                    String email = getCellString(row, 2);
                    String password = getCellString(row, 3);
                    if (password.isEmpty()) password = "default123";

                    int numTel = 0;
                    String numTelStr = getCellString(row, 4);
                    if (!numTelStr.isEmpty()) {
                        try { numTel = (int) Double.parseDouble(numTelStr); } catch (NumberFormatException ignored) {}
                    }

                    int roleId = 1;
                    String roleIdStr = getCellString(row, 5);
                    if (!roleIdStr.isEmpty()) {
                        try { roleId = (int) Double.parseDouble(roleIdStr); } catch (NumberFormatException ignored) {}
                    }

                    if (nom.isEmpty() || email.isEmpty()) {
                        result.errors.add("Ligne " + (row.getRowNum() + 1) + ": nom ou email vide");
                        result.failedRows++;
                        continue;
                    }

                    if (userService.emailExists(email)) {
                        result.errors.add("Ligne " + (row.getRowNum() + 1) + ": email deja existant (" + email + ")");
                        result.failedRows++;
                        continue;
                    }

                    boolean ok = userService.registerUser(nom, prenom, email, password, numTel, null, roleId);
                    if (ok) {
                        result.successRows++;
                    } else {
                        result.errors.add("Ligne " + (row.getRowNum() + 1) + ": erreur insertion DB");
                        result.failedRows++;
                    }
                } catch (Exception e) {
                    result.errors.add("Ligne " + (row.getRowNum() + 1) + ": " + e.getMessage());
                    result.failedRows++;
                }
            }
        } catch (Exception e) {
            result.errors.add("Erreur lecture fichier: " + e.getMessage());
            e.printStackTrace();
        }
        return result;
    }

    private String getCellString(Row row, int col) {
        Cell cell = row.getCell(col);
        if (cell == null) return "";
        switch (cell.getCellType()) {
            case STRING: return cell.getStringCellValue().trim();
            case NUMERIC: return String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN: return String.valueOf(cell.getBooleanCellValue());
            default: return "";
        }
    }

    // ========================
    // IMPORT RESULT
    // ========================
    public static class ImportResult {
        public int totalRows = 0;
        public int successRows = 0;
        public int failedRows = 0;
        public List<String> errors = new ArrayList<>();

        public String getSummary() {
            return String.format("Import termine: %d/%d reussis, %d echoues",
                    successRows, totalRows, failedRows);
        }
    }
}
