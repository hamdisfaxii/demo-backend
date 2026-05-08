package com.example.conges.service;

import com.example.conges.entity.History;
import com.example.conges.entity.History.ActionType;
import com.example.conges.repository.HistoryRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service pour gérer l'historique des actions sur les demandes de congés.
 * Enregistre automatiquement chaque action effectuée.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class HistoryService {

    private final HistoryRepository historyRepository;
    private final ObjectMapper objectMapper;

    /**
     * Enregistre une action dans l'historique.
     * Méthode générique appelée par les autres services.
     */
    @Transactional
    public void recordAction(Long userId, Long demandeId, ActionType actionType,
                             String description, String details,
                             String pays, String statut,
                             String userNom, String userPrenom, String userEmail) {
        try {
            History history = History.builder()
                    .userId(userId)
                    .demandeId(demandeId)
                    .actionType(actionType)
                    .description(description)
                    .details(details)
                    .pays(pays)
                    .statut(statut)
                    .userNom(userNom)
                    .userPrenom(userPrenom)
                    .userEmail(userEmail)
                    .ipAddress(getClientIpAddress())
                    .userAgent(getUserAgent())
                    .actionDate(LocalDateTime.now())
                    .build();

            historyRepository.save(history);
            log.info("Action enregistrée: {} pour l'utilisateur {}", actionType, userId);
        } catch (Exception e) {
            log.error("Erreur lors de l'enregistrement de l'historique", e);
            // Ne pas lever l'exception pour ne pas interrompre le flux métier
        }
    }

    /**
     * Surcharge pratique (si l'appelant a déjà un objet user/demande internes).
     * Ne crée AUCUNE relation JPA: on copie seulement des champs scalaires.
     */
    @Transactional
    public void recordAction(com.example.conges.entity.UserEntity user,
                             com.example.conges.entity.DemandeConge demande,
                             ActionType actionType,
                             String description,
                             String details) {
        Long userId = user != null ? user.getId() : null;
        if (userId == null) {
            // historique inutilisable sans userId -> ne pas casser le flux métier
            log.warn("Historique ignoré: userId manquant (actionType={})", actionType);
            return;
        }
        recordAction(
                userId,
                demande != null ? demande.getId() : null,
                actionType,
                description,
                details,
                user != null ? user.getPays() : null,
                demande != null && demande.getStatut() != null ? demande.getStatut().toString() : null,
                user != null ? user.getNom() : null,
                user != null ? user.getPrenom() : null,
                user != null ? user.getEmail() : null
        );
    }

    /**
     * Enregistre l'approbation d'une demande
     */
    @Transactional
    public void recordApproval(com.example.conges.entity.UserEntity user, com.example.conges.entity.DemandeConge demande, String approverName) {
        String description = "Demande approuvée par " + approverName;
        String details = buildDetails(
                "approver", approverName,
                "previousStatus", "EN_ATTENTE",
                "newStatus", demande.getStatut().toString()
        );
        recordAction(user, demande, ActionType.APPROVE, description, details);
    }

    /**
     * Enregistre le rejet d'une demande
     */
    @Transactional
    public void recordRejection(com.example.conges.entity.UserEntity user, com.example.conges.entity.DemandeConge demande, String rejectionReason) {
        String description = "Demande rejetée";
        String details = buildDetails(
                "reason", rejectionReason,
                "previousStatus", "EN_ATTENTE",
                "newStatus", demande.getStatut().toString()
        );
        recordAction(user, demande, ActionType.REJECT, description, details);
    }

    /**
     * Enregistre la création d'une demande
     */
    @Transactional
    public void recordCreation(com.example.conges.entity.UserEntity user, com.example.conges.entity.DemandeConge demande) {
        String description = "Demande de congé créée";
        String details = buildDetails(
                "typeConge", demande.getTypeConge().toString(),
                "dateDebut", demande.getDateDebut().toString(),
                "dateFin", demande.getDateFin().toString(),
                "nombreJours", String.valueOf(demande.getNombreJours()),
                "nombreJoursExact", String.valueOf(demande.getNombreJoursExactOrInt()),
                "startHalfDay", String.valueOf(demande.getStartHalfDay()),
                "endHalfDay", String.valueOf(demande.getEndHalfDay()),
                "approvedByAdminId", demande.getApprovedBy() != null ? String.valueOf(demande.getApprovedBy().getId()) : ""
        );
        recordAction(user, demande, ActionType.CREATE, description, details);
    }

    /**
     * Enregistre la modification d'une demande
     */
    @Transactional
    public void recordUpdate(com.example.conges.entity.UserEntity user, com.example.conges.entity.DemandeConge demande, String changes) {
        String description = "Demande mise à jour";
        recordAction(user, demande, ActionType.UPDATE, description, changes);
    }

    /**
     * Enregistre l'annulation d'une demande
     */
    @Transactional
    public void recordCancellation(com.example.conges.entity.UserEntity user, com.example.conges.entity.DemandeConge demande, String reason) {
        String description = "Demande annulée";
        String details = buildDetails(
                "reason", reason,
                "previousStatus", "ACCEPTE",
                "newStatus", "ANNULE"
        );
        recordAction(user, demande, ActionType.CANCEL, description, details);
    }

    /**
     * Enregistre une synchronisation avec Dolibarr
     */
    @Transactional
    public void recordDolibarrSync(com.example.conges.entity.UserEntity user, com.example.conges.entity.DemandeConge demande, String syncStatus) {
        String description = "Synchronisation Dolibarr";
        String details = buildDetails("syncStatus", syncStatus);
        recordAction(user, demande, ActionType.SYNCED_DOLIBARR, description, details);
    }

    /** Anti-doublon : un seul log par demande. */
    @Transactional
    public boolean recordSuperAdminsNotifiedOnce(com.example.conges.entity.UserEntity user, com.example.conges.entity.DemandeConge demande, String details) {
        try {
            if (demande == null || demande.getId() == null) return false;
            if (historyRepository.existsByDemandeIdAndActionType(demande.getId(), ActionType.SUPERADMINS_NOTIFIED)) {
                return false;
            }
            recordAction(
                    user,
                    demande,
                    ActionType.SUPERADMINS_NOTIFIED,
                    "Notification Super Admins (création demande)",
                    details
            );
            return true;
        } catch (RuntimeException ex) {
            // ne pas casser le flux métier
            return false;
        }
    }

    /**
     * Enregistre un export de document
     */
    @Transactional
    public void recordExport(com.example.conges.entity.UserEntity user, com.example.conges.entity.DemandeConge demande, String exportFormat, String filename) {
        String description = String.format("Export %s effectué", exportFormat);
        String details = buildDetails("format", exportFormat, "filename", filename);
        recordAction(user, demande, ActionType.EXPORTED, description, details);
    }

    /**
     * Enregistre une connexion utilisateur
     */
    @Transactional
    public void recordLogin(com.example.conges.entity.UserEntity user) {
        if (user == null || user.getId() == null) return;
        History history = History.builder()
                .userId(user.getId())
                .userNom(user.getNom())
                .userPrenom(user.getPrenom())
                .userEmail(user.getEmail())
                .actionType(ActionType.LOGIN)
                .description("Connexion utilisateur")
                .pays(user.getPays())
                .ipAddress(getClientIpAddress())
                .userAgent(getUserAgent())
                .actionDate(LocalDateTime.now())
                .build();

        historyRepository.save(history);
    }

    /**
     * Récupère l'historique complet avec filtres et pagination
     */
    @Transactional(readOnly = true)
    public Page<History> getHistory(Long userId, Long demandeId, ActionType actionType,
                                    String pays, LocalDateTime startDate, LocalDateTime endDate,
                                    Pageable pageable) {
        return historyRepository.searchHistory(userId, demandeId, actionType, pays, startDate, endDate, pageable);
    }

    /**
     * Récupère l'historique d'un utilisateur
     */
    @Transactional(readOnly = true)
    public Page<History> getUserHistory(Long userId, Pageable pageable) {
        return historyRepository.findByUserId(userId, pageable);
    }

    /**
     * Récupère l'historique d'une demande
     */
    @Transactional(readOnly = true)
    public Page<History> getDemandeHistory(Long demandeId, Pageable pageable) {
        return historyRepository.findByDemandeId(demandeId, pageable);
    }

    /**
     * Récupère l'historique pour export (non-paginé)
     */
    @Transactional(readOnly = true)
    public List<History> getHistoryForExport(Long userId, Long demandeId, ActionType actionType,
                                             String pays, LocalDateTime startDate, LocalDateTime endDate) {
        return historyRepository.searchHistoryForExport(
                userId,
                demandeId,
                actionType,
                pays,
                startDate,
                endDate
        );
    }

    /**
     * Récupère les statistiques d'actions
     */
    @Transactional(readOnly = true)
    public Map<ActionType, Long> getActionStatistics() {
        Map<ActionType, Long> stats = new HashMap<>();
        for (ActionType type : ActionType.values()) {
            stats.put(type, historyRepository.countByActionType(type));
        }
        return stats;
    }

    /**
     * Utilitaire pour extraire l'adresse IP du client
     */
    private String getClientIpAddress() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                String xForwardedFor = request.getHeader("X-Forwarded-For");
                if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
                    return xForwardedFor.split(",")[0];
                }
                return request.getRemoteAddr();
            }
        } catch (Exception e) {
            log.debug("Impossibilité d'extraire l'IP du client", e);
        }
        return "UNKNOWN";
    }

    /**
     * Utilitaire pour extraire le User-Agent
     */
    private String getUserAgent() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                return request.getHeader("User-Agent");
            }
        } catch (Exception e) {
            log.debug("Impossibilité d'extraire le User-Agent", e);
        }
        return "UNKNOWN";
    }

    /**
     * Utilitaire pour créer un objet JSON de détails
     */
    private String buildDetails(String... keyValues) {
        Map<String, String> details = new HashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            if (i + 1 < keyValues.length) {
                details.put(keyValues[i], keyValues[i + 1]);
            }
        }
        try {
            return objectMapper.writeValueAsString(details);
        } catch (Exception e) {
            log.warn("Erreur lors de la sérialisation JSON des détails", e);
            return "{}";
        }
    }
}
