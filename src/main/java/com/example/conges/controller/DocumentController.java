package com.example.conges.controller;

import com.example.conges.service.export.AttestationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDate;

/**
 * Controller REST pour la génération d'attestations et certificats
 */
@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
@Slf4j
public class DocumentController {

    private final AttestationService attestationService;

    /**
     * GET /api/documents/attestation/:demandeId
     * Génère une attestation de congé pour une demande spécifique
     */
    @GetMapping("/attestation/{demandeId}")
    @PreAuthorize("hasAnyRole('RH', 'ADMIN', 'EMPLOYEE')")
    public ResponseEntity<byte[]> generateLeaveAttestation(@PathVariable Long demandeId) {
        try {
            log.info("Génération d'attestation pour la demande {}", demandeId);
            
            byte[] attestation = attestationService.generateLeaveAttestation(demandeId);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, 
                            "attachment; filename=\"attestation_conge_" + demandeId + ".pdf\"")
                    .header(HttpHeaders.CONTENT_TYPE, "application/pdf")
                    .body(attestation);

        } catch (IOException e) {
            log.error("Erreur lors de la génération de l'attestation", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * GET /api/documents/certificate/:userId/:year
     * Génère un certificat de congés annuels
     */
    @GetMapping("/certificate/{userId}/{year}")
    @PreAuthorize("hasAnyRole('RH', 'ADMIN') or #userId == principal.id")
    public ResponseEntity<byte[]> generateAnnualCertificate(
            @PathVariable Long userId,
            @PathVariable int year) {
        try {
            log.info("Génération de certificat pour l'utilisateur {} pour l'année {}", userId, year);
            
            byte[] certificate = attestationService.generateAnnualLeavesCertificate(userId, year);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, 
                            "attachment; filename=\"certificat_conges_" + year + ".pdf\"")
                    .header(HttpHeaders.CONTENT_TYPE, "application/pdf")
                    .body(certificate);

        } catch (IOException e) {
            log.error("Erreur lors de la génération du certificat", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * GET /api/documents/planning/:userId
     * Génère un document de planification des congés
     * 
     * Query params:
     * - startDate: Date de début (ISO format)
     * - endDate: Date de fin (ISO format)
     */
    @GetMapping("/planning/{userId}")
    @PreAuthorize("hasAnyRole('RH', 'ADMIN') or #userId == principal.id")
    public ResponseEntity<byte[]> generatePlanningDocument(
            @PathVariable Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        try {
            log.info("Génération du document de planification pour l'utilisateur {} du {} au {}", userId, startDate, endDate);
            
            byte[] planning = attestationService.generatePlanningDocument(userId, startDate, endDate);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, 
                            "attachment; filename=\"planning_conges_" + System.currentTimeMillis() + ".pdf\"")
                    .header(HttpHeaders.CONTENT_TYPE, "application/pdf")
                    .body(planning);

        } catch (IOException e) {
            log.error("Erreur lors de la génération du document de planification", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
