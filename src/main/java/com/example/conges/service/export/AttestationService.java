package com.example.conges.service.export;

import com.example.conges.entity.DemandeConge;
import com.example.conges.entity.UserEntity;
import com.example.conges.repository.DemandeCongeRepository;
import com.itextpdf.io.font.PdfEncodings;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Service pour générer les attestations de congés et certificats personnalisés
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AttestationService {

    private final DemandeCongeRepository demandeCongeRepository;
    private static final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter dateFormatterComplete = DateTimeFormatter.ofPattern("dd MMMM yyyy", java.util.Locale.FRENCH);

    /**
     * Génère une attestation de congé pour une demande spécifique
     */
    public byte[] generateLeaveAttestation(Long demandeId) throws IOException {
        DemandeConge demande = demandeCongeRepository.findById(demandeId)
                .orElseThrow(() -> new IllegalArgumentException("Demande introuvable"));

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(baos);
        PdfDocument pdfDoc = new PdfDocument(writer);
        Document document = new Document(pdfDoc, PageSize.A4);

        // Ajouter le contenu
        addAttestationContent(document, demande);

        document.close();
        log.info("Attestation générée pour la demande {}", demandeId);

        return baos.toByteArray();
    }

    /**
     * Génère un certificat de congé payé pour un bilan annuel
     */
    public byte[] generateAnnualLeavesCertificate(Long userId, int year) throws IOException {
        List<DemandeConge> demandes = demandeCongeRepository.findByUserId(userId);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(baos);
        PdfDocument pdfDoc = new PdfDocument(writer);
        Document document = new Document(pdfDoc, PageSize.A4);

        UserEntity user = demandes.isEmpty() ? null : demandes.get(0).getUser();
        if (user == null) {
            throw new IllegalArgumentException("Utilisateur introuvable");
        }

        // Ajouter le contenu du certificat
        addAnnualCertificateContent(document, user, demandes, year);

        document.close();
        log.info("Certificat annuel généré pour l'utilisateur {}", userId);

        return baos.toByteArray();
    }

    /**
     * Génère un document de congés planifiés pour une période
     */
    public byte[] generatePlanningDocument(Long userId, LocalDate startDate, LocalDate endDate) throws IOException {
        List<DemandeConge> demandes = demandeCongeRepository.findByDateDebutBetween(startDate, endDate);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(baos);
        PdfDocument pdfDoc = new PdfDocument(writer);
        Document document = new Document(pdfDoc, PageSize.A4);

        UserEntity user = demandes.isEmpty() ? null : demandes.get(0).getUser();
        if (user == null) {
            throw new IllegalArgumentException("Utilisateur introuvable");
        }

        // Ajouter le contenu du document de planification
        addPlanningContent(document, user, demandes, startDate, endDate);

        document.close();
        log.info("Document de planification généré de {} à {}", startDate, endDate);

        return baos.toByteArray();
    }

    // =================== CONTENU DES DOCUMENTS ===================

    private void addAttestationContent(Document document, DemandeConge demande) throws IOException {
        PdfFont fontBold = PdfFontFactory.createFont(PdfEncodings.IDENTITY_H, PdfEncodings.UNICODE_BIG);
        PdfFont fontNormal = PdfFontFactory.createFont(PdfEncodings.IDENTITY_H, PdfEncodings.UNICODE_BIG);

        // En-tête
        Paragraph header = new Paragraph("ATTESTATION DE CONGÉ PAYÉ")
                .setFont(fontBold)
                .setFontSize(16)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(20);
        document.add(header);

        Paragraph separator = new Paragraph("═════════════════════════════════════════════")
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(20);
        document.add(separator);

        // Corps du document
        LocalDate today = LocalDate.now();
        String dateStr = today.format(dateFormatterComplete);

        Paragraph date = new Paragraph("Établi le : " + dateStr)
                .setMarginBottom(20);
        document.add(date);

        // Informations de l'employé
        Paragraph employeeInfo = new Paragraph()
                .add("Certifié que l'employé(e) ")
                .add(new Paragraph(demande.getUser().getPrenom() + " " + demande.getUser().getNom())
                        .setFont(fontBold))
                .add(", numéro de dossier : ")
                .add(demande.getUser().getId().toString())
                .setMarginBottom(15);
        document.add(employeeInfo);

        // Détails du congé
        Paragraph leaveDetails = new Paragraph()
                .add("a pris un congé de type ")
                .add(new Paragraph(demande.getTypeConge().toString())
                        .setFont(fontBold))
                .add(" pour une durée de ")
                .add(new Paragraph(demande.getNombreJours() + " jour(s)")
                        .setFont(fontBold))
                .add(" du ")
                .add(demande.getDateDebut().format(dateFormatter))
                .add(" au ")
                .add(demande.getDateFin().format(dateFormatter))
                .setMarginBottom(15);
        document.add(leaveDetails);

        // Objet du congé
        if (demande.getMotif() != null && !demande.getMotif().isEmpty()) {
            Paragraph reason = new Paragraph()
                    .add("Motif : ")
                    .add(demande.getMotif())
                    .setMarginBottom(20);
            document.add(reason);
        }

        // Signature
        Paragraph signature = new Paragraph()
                .add("\n\n")
                .add("Fait à titre informatif,\n")
                .add("Le Département Ressources Humaines")
                .setMarginTop(30);
        document.add(signature);
    }

    private void addAnnualCertificateContent(Document document, UserEntity user, List<DemandeConge> demandes, int year) throws IOException {
        PdfFont fontBold = PdfFontFactory.createFont(PdfEncodings.IDENTITY_H, PdfEncodings.UNICODE_BIG);

        // En-tête
        Paragraph header = new Paragraph("CERTIFICAT DE CONGÉS")
                .setFont(fontBold)
                .setFontSize(16)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(20);
        document.add(header);

        Paragraph year_p = new Paragraph("Année : " + year)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(20);
        document.add(year_p);

        // Informations de l'employé
        Paragraph employeeInfo = new Paragraph()
                .add("Employé : ")
                .add(new Paragraph(user.getPrenom() + " " + user.getNom())
                        .setFont(fontBold))
                .add("\nDépartement : ")
                .add(user.getDepartement() != null ? user.getDepartement() : "N/A")
                .setMarginBottom(20);
        document.add(employeeInfo);

        // Résumé des congés
        long totalDays = demandes.stream()
                .filter(d -> d.getDateDebut().getYear() == year)
                .mapToInt(DemandeConge::getNombreJours)
                .sum();

        Paragraph summary = new Paragraph()
                .add("Total de congés pris en " + year + " : ")
                .add(new Paragraph(totalDays + " jour(s)")
                        .setFont(fontBold))
                .setMarginBottom(20);
        document.add(summary);

        // Table des détails
        Table table = new Table(UnitValue.createPercentArray(new float[]{20, 15, 15, 15, 35}))
                .setWidth(UnitValue.createPercentValue(100));

        // En-tête de tableau
        table.addCell(createHeaderCell("Type de congé"));
        table.addCell(createHeaderCell("Début"));
        table.addCell(createHeaderCell("Fin"));
        table.addCell(createHeaderCell("Jours"));
        table.addCell(createHeaderCell("Statut"));

        // Remplir le tableau
        for (DemandeConge d : demandes) {
            if (d.getDateDebut().getYear() == year) {
                table.addCell(new Cell().add(new Paragraph(d.getTypeConge().toString())));
                table.addCell(new Cell().add(new Paragraph(d.getDateDebut().format(dateFormatter))));
                table.addCell(new Cell().add(new Paragraph(d.getDateFin().format(dateFormatter))));
                table.addCell(new Cell().add(new Paragraph(String.valueOf(d.getNombreJours()))));
                table.addCell(new Cell().add(new Paragraph(d.getStatut().toString())));
            }
        }

        document.add(table);
    }

    private void addPlanningContent(Document document, UserEntity user, List<DemandeConge> demandes, LocalDate start, LocalDate end) throws IOException {
        PdfFont fontBold = PdfFontFactory.createFont(PdfEncodings.IDENTITY_H, PdfEncodings.UNICODE_BIG);

        // En-tête
        Paragraph header = new Paragraph("PLANNING DES CONGÉS")
                .setFont(fontBold)
                .setFontSize(16)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(20);
        document.add(header);

        // Période
        Paragraph period = new Paragraph()
                .add("Période : ")
                .add(start.format(dateFormatter) + " à " + end.format(dateFormatter))
                .setMarginBottom(20);
        document.add(period);

        // Employé
        Paragraph employeeInfo = new Paragraph()
                .add("Employé : ")
                .add(new Paragraph(user.getPrenom() + " " + user.getNom())
                        .setFont(fontBold))
                .setMarginBottom(20);
        document.add(employeeInfo);

        // Table de planning
        Table table = new Table(UnitValue.createPercentArray(new float[]{25, 15, 15, 20, 15, 10}))
                .setWidth(UnitValue.createPercentValue(100));

        // En-tête de tableau
        table.addCell(createHeaderCell("Type"));
        table.addCell(createHeaderCell("Début"));
        table.addCell(createHeaderCell("Fin"));
        table.addCell(createHeaderCell("Motif"));
        table.addCell(createHeaderCell("Jours"));
        table.addCell(createHeaderCell("Status"));

        // Remplir le tableau
        for (DemandeConge d : demandes) {
            table.addCell(new Cell().add(new Paragraph(d.getTypeConge().toString())));
            table.addCell(new Cell().add(new Paragraph(d.getDateDebut().format(dateFormatter))));
            table.addCell(new Cell().add(new Paragraph(d.getDateFin().format(dateFormatter))));
            table.addCell(new Cell().add(new Paragraph(d.getMotif() != null ? d.getMotif().substring(0, Math.min(30, d.getMotif().length())) : "")));
            table.addCell(new Cell().add(new Paragraph(String.valueOf(d.getNombreJours()))));
            table.addCell(new Cell().add(new Paragraph(d.getStatut().toString())));
        }

        document.add(table);
    }

    /**
     * Crée une cellule d'en-tête de tableau
     */
    private Cell createHeaderCell(String text) {
        return new Cell()
                .add(new Paragraph(text).setBold())
                .setTextAlignment(TextAlignment.CENTER);
    }
}
