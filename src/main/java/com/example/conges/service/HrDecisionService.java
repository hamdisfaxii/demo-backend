package com.example.conges.service;

import com.example.conges.dto.DemandeCongeResponse;
import com.example.conges.dto.hr.HrDecisionRequest;
import com.example.conges.dto.hr.HrLeaveRequestResponse;
import com.example.conges.entity.DemandeConge;
import com.example.conges.entity.Role;
import com.example.conges.entity.StatutConge;
import com.example.conges.entity.UserEntity;
import com.example.conges.repository.DemandeCongeRepository;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class HrDecisionService {

    private final DemandeCongeRepository demandeCongeRepository;
    private final CongeService congeService;

    @Transactional(readOnly = true)
    public List<HrLeaveRequestResponse> getPendingRequests(
            UserEntity actor,
            String employee,
            String country,
            String department,
            LocalDate startDate,
            LocalDate endDate
    ) {
        validateHrOrAdmin(actor);
        String effectiveCountry = resolveCountryFilter(actor, country);
        return demandeCongeRepository.findPendingForHrPanel(
                normalizeOptional(employee),
                effectiveCountry,
                normalizeOptional(department),
                startDate,
                endDate
        ).stream().map(this::toHrResponse).toList();
    }

    /**
     * Historique RH filtrable (tous statuts ou PENDING/APPROVED/REJECTED côté API).
     */
    @Transactional(readOnly = true)
    public List<HrLeaveRequestResponse> getHistoryRequests(
            UserEntity actor,
            String statusFilter,
            String employee,
            String country,
            String department,
            LocalDate startDate,
            LocalDate endDate
    ) {
        validateHrOrAdmin(actor);
        String effectiveCountry = resolveCountryFilter(actor, country);
        StatutConge statut = parseApiStatus(statusFilter);
        return demandeCongeRepository.findHistoryForHrPanel(
                statut,
                normalizeOptional(employee),
                effectiveCountry,
                normalizeOptional(department),
                startDate,
                endDate
        ).stream().map(this::toHrResponse).toList();
    }

    /** null = tous statuts ; accepte synonymes envoyés par le front React. */
    private StatutConge parseApiStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        String u = status.trim().toUpperCase(Locale.ROOT);
        if ("ALL".equals(u)) {
            return null;
        }
        if ("PENDING".equals(u) || "EN_ATTENTE".equals(u)) {
            return StatutConge.EN_ATTENTE;
        }
        if ("APPROVED".equals(u) || "ACCEPTE".equals(u) || "ACCEPTED".equals(u)) {
            return StatutConge.ACCEPTE;
        }
        if ("REJECTED".equals(u) || "REFUSE".equals(u) || "REFUS".equals(u)) {
            return StatutConge.REFUSE;
        }
        throw new IllegalArgumentException("Statut filtre invalide: " + status);
    }

    @Transactional
    public DemandeCongeResponse processDecision(
            Long demandeId,
            UserEntity actor,
            HrDecisionRequest request
    ) {
        validateHrOrAdmin(actor);
        DemandeConge demande = demandeCongeRepository.findById(demandeId)
                .orElseThrow(() -> new EntityNotFoundException("Demande introuvable"));
        enforceCountryAccess(actor, demande);
        boolean approve = parseDecision(request.getAction());
        String comment = request.getComment() == null ? null : request.getComment().trim();
        return congeService.validerDemande(demandeId, actor.getId(), approve, comment);
    }

    @Transactional(readOnly = true)
    public HrLeaveRequestResponse getRequestDetails(Long demandeId, UserEntity actor) {
        validateHrOrAdmin(actor);
        DemandeConge demande = demandeCongeRepository.findById(demandeId)
                .orElseThrow(() -> new EntityNotFoundException("Demande introuvable"));
        enforceCountryAccess(actor, demande);
        return toHrResponse(demande);
    }

    @Transactional(readOnly = true)
    public Map<String, Long> getStats(UserEntity actor) {
        validateHrOrAdmin(actor);
        String actorCountry = normalizeOptional(actor.getPays());
        if (actor.getRole() != Role.ADMIN && actorCountry == null) {
            throw new AccessDeniedException("Pays utilisateur introuvable.");
        }
        Map<String, Long> stats = new HashMap<>();
        long pending = actor.getRole() == Role.ADMIN
                ? demandeCongeRepository.countByStatut(com.example.conges.entity.StatutConge.EN_ATTENTE)
                : demandeCongeRepository.countByStatutAndUser_PaysIgnoreCase(
                        com.example.conges.entity.StatutConge.EN_ATTENTE,
                        actorCountry
                );
        long approved = actor.getRole() == Role.ADMIN
                ? demandeCongeRepository.countByStatut(com.example.conges.entity.StatutConge.ACCEPTE)
                : demandeCongeRepository.countByStatutAndUser_PaysIgnoreCase(
                        com.example.conges.entity.StatutConge.ACCEPTE,
                        actorCountry
                );
        long rejected = actor.getRole() == Role.ADMIN
                ? demandeCongeRepository.countByStatut(com.example.conges.entity.StatutConge.REFUSE)
                : demandeCongeRepository.countByStatutAndUser_PaysIgnoreCase(
                        com.example.conges.entity.StatutConge.REFUSE,
                        actorCountry
                );
        stats.put("pending", pending);
        stats.put("approved", approved);
        stats.put("rejected", rejected);
        stats.put("total", pending + approved + rejected);
        return stats;
    }

    private boolean parseDecision(String action) {
        String normalized = action == null ? "" : action.trim().toUpperCase(Locale.ROOT);
        if ("APPROVE".equals(normalized) || "APPROVED".equals(normalized)) {
            return true;
        }
        if ("REJECT".equals(normalized) || "REJECTED".equals(normalized)) {
            return false;
        }
        throw new IllegalArgumentException("Action invalide. Utilisez APPROVE ou REJECT");
    }

    private String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private void validateHrOrAdmin(UserEntity actor) {
        if (actor == null || (actor.getRole() != Role.RH && actor.getRole() != Role.ADMIN)) {
            throw new AccessDeniedException("Accès réservé aux rôles RH/ADMIN");
        }
    }

    private String resolveCountryFilter(UserEntity actor, String requestedCountry) {
        if (actor.getRole() == Role.ADMIN) {
            return normalizeOptional(requestedCountry);
        }
        String actorCountry = normalizeOptional(actor.getPays());
        if (actorCountry == null) {
            throw new AccessDeniedException("Pays utilisateur introuvable.");
        }
        return actorCountry;
    }

    private void enforceCountryAccess(UserEntity actor, DemandeConge demande) {
        if (actor.getRole() == Role.ADMIN) {
            return;
        }
        String actorCountry = normalizeOptional(actor.getPays());
        String requestCountry = demande.getUser() == null ? null : normalizeOptional(demande.getUser().getPays());
        if (actorCountry == null || requestCountry == null || !actorCountry.equalsIgnoreCase(requestCountry)) {
            throw new AccessDeniedException("Accès refusé: demande d'un autre pays.");
        }
    }

    private HrLeaveRequestResponse toHrResponse(DemandeConge d) {
        UserEntity u = d.getUser();
        return HrLeaveRequestResponse.builder()
                .id(d.getId())
                .typeConge(d.getTypeConge())
                .statut(d.getStatut())
                .dateDebut(d.getDateDebut())
                .dateFin(d.getDateFin())
                .nombreJours(d.getNombreJours())
                .motif(d.getMotif())
                .commentaireRh(d.getCommentaireRh())
                .dateSoumission(d.getDateSoumission())
                .workflowCode(d.getWorkflowCode())
                .currentStepOrder(d.getCurrentStepOrder())
                .currentStepType(d.getCurrentStepType() == null ? null : d.getCurrentStepType().name())
                .employe(HrLeaveRequestResponse.EmployeInfo.builder()
                        .id(u.getId())
                        .nom(u.getNom())
                        .prenom(u.getPrenom())
                        .email(u.getEmail())
                        .country(u.getPays())
                        .department(u.getDepartement())
                        .build())
                .build();
    }
}
