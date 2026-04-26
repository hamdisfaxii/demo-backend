package com.example.conges.service.export;

import com.example.conges.entity.History;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Service pour générer des rapports Excel
 * des demandes de congés et historiques.
 */
@Service
@Slf4j
public class ExcelExportService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /**
     * Génère un fichier Excel contenant l'historique des actions
     */
    public byte[] generateHistoryExcel(List<History> historyList, String sheetName) throws IOException {
        XSSFWorkbook workbook = new XSSFWorkbook();
        XSSFSheet sheet = workbook.createSheet(sheetName != null ? sheetName : "Historique");

        // Styles
        XSSFCellStyle headerStyle = createHeaderStyle(workbook);
        XSSFCellStyle cellStyle = createCellStyle(workbook);
        XSSFCellStyle dateStyle = createDateStyle(workbook);

        // Headers
        XSSFRow headerRow = sheet.createRow(0);
        String[] headers = {"Date", "Utilisateur", "Email", "Action", "Description", "Demande ID", "Pays", "Statut", "IP Address"};

        for (int i = 0; i < headers.length; i++) {
            XSSFCell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // Données
        int rowNum = 1;
        for (History history : historyList) {
            XSSFRow row = sheet.createRow(rowNum++);

            // Date
            XSSFCell dateCell = row.createCell(0);
            dateCell.setCellValue(history.getActionDate().format(
                    DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
            dateCell.setCellStyle(dateStyle);

            // Utilisateur
            row.createCell(1).setCellValue(
                    history.getUser().getPrenom() + " " + history.getUser().getNom());
            row.getCell(1).setCellStyle(cellStyle);

            // Email
            row.createCell(2).setCellValue(history.getUser().getEmail());
            row.getCell(2).setCellStyle(cellStyle);

            // Action
            row.createCell(3).setCellValue(history.getActionType().toString());
            row.getCell(3).setCellStyle(cellStyle);

            // Description
            row.createCell(4).setCellValue(history.getDescription() != null ? history.getDescription() : "-");
            row.getCell(4).setCellStyle(cellStyle);

            // Demande ID
            row.createCell(5).setCellValue(
                    history.getDemande() != null ? String.valueOf(history.getDemande().getId()) : "-");
            row.getCell(5).setCellStyle(cellStyle);

            // Pays
            row.createCell(6).setCellValue(history.getPays() != null ? history.getPays() : "-");
            row.getCell(6).setCellStyle(cellStyle);

            // Statut
            row.createCell(7).setCellValue(history.getStatut() != null ? history.getStatut() : "-");
            row.getCell(7).setCellStyle(cellStyle);

            // IP Address
            row.createCell(8).setCellValue(history.getIpAddress() != null ? history.getIpAddress() : "-");
            row.getCell(8).setCellStyle(cellStyle);
        }

        // Ajuste la largeur des colonnes
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }

        // Gel la première ligne (headers)
        sheet.createFreezePane(0, 1);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        workbook.write(baos);
        workbook.close();

        return baos.toByteArray();
    }

    /**
     * Génère un fichier Excel contenant un rapport RH
     */
    public byte[] generateRhReport(List<History> historyList) throws IOException {
        XSSFWorkbook workbook = new XSSFWorkbook();
        XSSFSheet sheet = workbook.createSheet("Rapport RH");

        XSSFCellStyle headerStyle = createHeaderStyle(workbook);
        XSSFCellStyle cellStyle = createCellStyle(workbook);

        // Titre
        XSSFRow titleRow = sheet.createRow(0);
        XSSFCell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("RAPPORT RH - GESTION DES CONGÉS");
        XSSFCellStyle titleStyle = workbook.createCellStyle();
        titleStyle.getFont().setBold(true);
        titleStyle.getFont().setFontHeightInPoints((short) 14);
        titleCell.setCellStyle(titleStyle);

        // Info de génération
        XSSFRow infoRow = sheet.createRow(2);
        infoRow.createCell(0).setCellValue("Date de génération: " + LocalDate.now().format(DATE_FORMATTER));

        // Headers du tableau
        XSSFRow headerRow = sheet.createRow(4);
        String[] headers = {"Date", "Utilisateur", "Action", "Type Congé", "Jours", "Pays", "Statut"};

        for (int i = 0; i < headers.length; i++) {
            XSSFCell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // Remplir les données
        int rowNum = 5;
        for (History history : historyList) {
            if (history.getDemande() != null) {
                XSSFRow row = sheet.createRow(rowNum++);

                row.createCell(0).setCellValue(
                        history.getActionDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
                row.getCell(0).setCellStyle(cellStyle);

                row.createCell(1).setCellValue(
                        history.getUser().getPrenom() + " " + history.getUser().getNom());
                row.getCell(1).setCellStyle(cellStyle);

                row.createCell(2).setCellValue(history.getActionType().toString());
                row.getCell(2).setCellStyle(cellStyle);

                row.createCell(3).setCellValue(
                        history.getDemande().getTypeConge().toString());
                row.getCell(3).setCellStyle(cellStyle);

                row.createCell(4).setCellValue(history.getDemande().getNombreJours());
                row.getCell(4).setCellStyle(cellStyle);

                row.createCell(5).setCellValue(history.getPays() != null ? history.getPays() : "-");
                row.getCell(5).setCellStyle(cellStyle);

                row.createCell(6).setCellValue(history.getStatut());
                row.getCell(6).setCellStyle(cellStyle);
            }
        }

        // Autosize colonnes
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        workbook.write(baos);
        workbook.close();

        return baos.toByteArray();
    }

    /**
     * Crée un style pour les entêtes
     */
    private XSSFCellStyle createHeaderStyle(XSSFWorkbook workbook) {
        XSSFCellStyle style = workbook.createCellStyle();
        XSSFFont font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        font.setFontHeightInPoints((short) 11);
        style.setFont(font);
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setFillForegroundColor(IndexedColors.BLUE.getIndex());
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    /**
     * Crée un style pour les cellules
     */
    private XSSFCellStyle createCellStyle(XSSFWorkbook workbook) {
        XSSFCellStyle style = workbook.createCellStyle();
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    /**
     * Crée un style pour les dates
     */
    private XSSFCellStyle createDateStyle(XSSFWorkbook workbook) {
        XSSFCellStyle style = createCellStyle(workbook);
        style.setDataFormat(workbook.createDataFormat().getFormat("dd/mm/yyyy hh:mm"));
        return style;
    }
}
