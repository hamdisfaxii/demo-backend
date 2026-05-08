package com.example.conges.service.export;

import com.example.conges.entity.History;
import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Service pour générer une attestation ou rapport PDF
 * des demandes de congés et historiques.
 */
@Service
@Slf4j
public class PdfExportService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /**
     * Génère le PDF de l'historique des actions
     */
    public byte[] generateHistoryReport(List<History> historyList, String title) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(baos);
        PdfDocument pdfDoc = new PdfDocument(writer);
        Document document = new Document(pdfDoc);

        // Configuration des polices
        PdfFont titleFont = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
        PdfFont headerFont = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
        PdfFont normalFont = PdfFontFactory.createFont(StandardFonts.HELVETICA);

        // Titre
        Paragraph titleParagraph = new Paragraph(title)
                .setFont(titleFont)
                .setFontSize(16)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(20);
        document.add(titleParagraph);

        // Date de génération
        Paragraph dateParagraph = new Paragraph(
                "Généré le: " + LocalDate.now().format(DATE_FORMATTER))
                .setFont(normalFont)
                .setFontSize(10)
                .setTextAlignment(TextAlignment.RIGHT)
                .setMarginBottom(20);
        document.add(dateParagraph);

        // Table d'historique
        Table table = new Table(new float[]{1, 1.5f, 1.5f, 2f, 1.5f, 1.5f})
                .setWidth(UnitValue.createPercentValue(100));

        // En-têtes
        addTableHeader(table, headerFont, "Date", "Utilisateur", "Action", "Description", "Demande ID", "Statut");

        // Lignes du tableau
        for (History history : historyList) {
            table.addCell(new Cell()
                    .add(new Paragraph(history.getActionDate().format(
                            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))))
                    .setFont(normalFont)
                    .setFontSize(9));

            String userName = ((history.getUserPrenom() != null ? history.getUserPrenom() : "") + " " +
                    (history.getUserNom() != null ? history.getUserNom() : "")).trim();
            if (userName.isEmpty()) userName = history.getUserEmail() != null ? history.getUserEmail() : String.valueOf(history.getUserId());
            table.addCell(new Cell()
                    .add(new Paragraph(userName))
                    .setFont(normalFont)
                    .setFontSize(9));

            table.addCell(new Cell()
                    .add(new Paragraph(history.getActionType().toString()))
                    .setFont(normalFont)
                    .setFontSize(9));

            table.addCell(new Cell()
                    .add(new Paragraph(history.getDescription() != null ? history.getDescription() : "-"))
                    .setFont(normalFont)
                    .setFontSize(9));

            String demandeId = history.getDemandeId() != null ? history.getDemandeId().toString() : "-";
            table.addCell(new Cell()
                    .add(new Paragraph(demandeId))
                    .setFont(normalFont)
                    .setFontSize(9));

            table.addCell(new Cell()
                    .add(new Paragraph(history.getStatut() != null ? history.getStatut() : "-"))
                    .setFont(normalFont)
                    .setFontSize(9));
        }

        document.add(table);

        // Footer
        Paragraph footerParagraph = new Paragraph(
                "Rapport confidentiel - Système de Gestion des Congés")
                .setFont(normalFont)
                .setFontSize(8)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginTop(20);
        document.add(footerParagraph);

        document.close();
        return baos.toByteArray();
    }

    /**
     * Génère un PDF d'attestation de congés pour un utilisateur
     */
    public byte[] generateLeaveAttestation(String userName, String email, String pays, int totalDays,
                                          LocalDate startDate, LocalDate endDate) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(baos);
        PdfDocument pdfDoc = new PdfDocument(writer);
        Document document = new Document(pdfDoc);

        PdfFont titleFont = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
        PdfFont normalFont = PdfFontFactory.createFont(StandardFonts.HELVETICA);

        // En-tête
        Paragraph header = new Paragraph("ATTESTATION DE CONGÉS")
                .setFont(titleFont)
                .setFontSize(18)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(30);
        document.add(header);

        // Corps du document
        Paragraph para1 = new Paragraph("Nous certifions par la présente que " + userName +
                " (Email: " + email + ") a pris congé")
                .setFont(normalFont)
                .setFontSize(11)
                .setMarginBottom(10);
        document.add(para1);

        Paragraph para2 = new Paragraph("Période: Du " + startDate.format(DATE_FORMATTER) +
                " au " + endDate.format(DATE_FORMATTER))
                .setFont(normalFont)
                .setFontSize(11)
                .setMarginBottom(10);
        document.add(para2);

        Paragraph para3 = new Paragraph("Nombre de jours: " + totalDays + " jours")
                .setFont(normalFont)
                .setFontSize(11)
                .setMarginBottom(10);
        document.add(para3);

        Paragraph para4 = new Paragraph("Pays: " + pays)
                .setFont(normalFont)
                .setFontSize(11)
                .setMarginBottom(30);
        document.add(para4);

        // Lieu, date et signature
        Paragraph signature = new Paragraph("Date: " + LocalDate.now().format(DATE_FORMATTER) + "\n\n" +
                "________________________\nSignature RH")
                .setFont(normalFont)
                .setFontSize(10)
                .setMarginTop(50);
        document.add(signature);

        document.close();
        return baos.toByteArray();
    }

    /**
     * Ajoute une ligne d'en-tête au tableau
     */
    private void addTableHeader(Table table, PdfFont font, String... headers) {
        for (String header : headers) {
            Cell cell = new Cell()
                    .add(new Paragraph(header))
                    .setFont(font)
                    .setFontSize(10)
                    .setTextAlignment(TextAlignment.CENTER);
            table.addHeaderCell(cell);
        }
    }
}
