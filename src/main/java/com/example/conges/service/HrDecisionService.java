package com.example.conges.service;

import com.example.conges.dto.DemandeCongeResponse;
import com.example.conges.dto.hr.HrDecisionRequest;
import com.example.conges.dto.hr.HrLeaveRequestResponse;
import com.example.conges.entity.DemandeConge;
import com.example.conges.entity.Role;
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
        return demandeCongeRepository.findPendingForHrPanel(
                normalizeOptional(employee),
                normalizeOptional(country),
                normalizeOptional(department),
                startDate,
                endDate
        ).stream().map(this::toHrResponse).toList();
    }

    @Transactional
    public DemandeCongeResponse processDecision(
            Long demandeId,
            UserEntity actor,
            HrDecisionRequest request
    ) {
        validateHrOrAdmin(actor);
        boolean approve = parseDecision(request.getAction());
        String comment = request.getComment() == null ? null : request.getComment().trim();
        return congeService.validerDemande(demandeId, actor.getId(), approve, comment);
    }

    @Transactional(readOnly = true)
    public HrLeaveRequestResponse getRequestDetails(Long demandeId, UserEntity actor) {
        validateHrOrAdmin(actor);
        DemandeConge demande = demandeCongeRepository.findById(demandeId)
                .orElseThrow(() -> new EntityNotFoundException("Demande introuvable"));
        return toHrResponse(demande);
    }

    @Transactional(readOnly = true)
    public Map<String, Long> getStats(UserEntity actor) {
        validateHrOrAdmin(actor);
        Map<String, Long> stats = new HashMap<>();
        long pending = demandeCongeRepository.countByStatut(com.example.conges.entity.StatutConge.EN_ATTENTE);
        long approved = demandeCongeRepository.countByStatut(com.example.conges.entity.StatutConge.ACCEPTE);
        long rejected = demandeCongeRepository.countByStatut(com.example.conges.entity.StatutConge.REFUSE);
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
