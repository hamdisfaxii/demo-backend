package com.example.conges.service;

import com.example.conges.dto.DemandeCongeRequest;
import com.example.conges.dto.DemandeCongeResponse;
import com.example.conges.dto.SoldeCongeResponse;
import com.example.conges.dto.StatistiquesRhResponse;
import com.example.conges.entity.DemandeConge;
import com.example.conges.entity.EmployeeLeaveAllocation;
import com.example.conges.entity.LeaveType;
import com.example.conges.entity.Role;
import com.example.conges.entity.StatutConge;
import com.example.conges.entity.TypeConge;
import com.example.conges.entity.UserEntity;
import com.example.conges.repository.DemandeCongeRepository;
import com.example.conges.repository.EmployeeLeaveAllocationRepository;
import com.example.conges.repository.JoursPrisParTypeProjection;
import com.example.conges.repository.LeaveTypeRepository;
import com.example.conges.repository.UserRepository;
import javax.persistence.EntityNotFoundException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import org.springframework.util.StringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CongeService {

    private static final EnumSet<StatutConge> STATUTS_COMPTABILISES_SOLDE =
            EnumSet.of(StatutConge.ACCEPTE, StatutConge.EN_ATTENTE);

    private static final EnumSet<StatutConge> STATUTS_HISTORIQUE =
            EnumSet.of(StatutConge.ACCEPTE, StatutConge.REFUSE, StatutConge.ANNULE);

    private final DemandeCongeRepository demandeCongeRepository;
    private final UserRepository userRepository;
    private final HistoryService historyService;
    private final WorkflowService workflowService;
    private final CountryPolicyService countryPolicyService;
    private final DolibarrService dolibarrService;
    private final EmployeeLeaveAllocationRepository employeeLeaveAllocationRepository;
    private final LeaveTypeRepository leaveTypeRepository;
    private final HrWorkScheduleService hrWorkScheduleService;

    @Transactional
    public DemandeCongeResponse creerDemande(Long userId, DemandeCongeRequest request) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Utilisateur introuvable"));

        if (request.getDateFin().isBefore(request.getDateDebut())) {
            throw new IllegalArgumentException("La date de fin doit être après ou égale à la date de début");
        }

        int joursOuvrables = calculerJoursOuvrables(request.getDateDebut(), request.getDateFin());
        if (joursOuvrables <= 0) {
            throw new IllegalArgumentException("Aucun jour ouvrable dans la période choisie");
        }

        hrWorkScheduleService.validatePermissionWithinWorkingHours(user, request);

        verifierSoldeDisponible(userId, request.getTypeConge(), joursOuvrables);

        DemandeConge demande = DemandeConge.builder()
                .user(user)
                .typeConge(request.getTypeConge())
                .dateDebut(request.getDateDebut())
                .dateFin(request.getDateFin())
                .nombreJours(joursOuvrables)
                .motif(request.getMotif())
                .statut(StatutConge.EN_ATTENTE)
                .build();
        workflowService.initializeWorkflow(demande);

        DemandeConge saved = demandeCongeRepository.save(demande);
        Long dolibarrLeaveId = dolibarrService.pushLeaveRequest(saved);
        if (dolibarrLeaveId != null) {
            saved.setDolibarrLeaveRequestId(dolibarrLeaveId);
            saved = demandeCongeRepository.save(saved);
            historyService.recordDolibarrSync(user, saved, "OUTBOUND_CREATED");
        }
        log.info("Demande de congé créée id={} pour userId={}", saved.getId(), userId);
        
        // Enregistrer dans l'historique
        historyService.recordCreation(user, saved);
        
        return toResponse(saved);
    }

    @Transactional
    public DemandeCongeResponse annulerDemande(Long demandeId, Long userId) {
        DemandeConge demande = demandeCongeRepository.findById(demandeId)
                .orElseThrow(() -> new EntityNotFoundException("Demande introuvable"));

        if (!demande.getUser().getId().equals(userId)) {
            throw new AccessDeniedException("Cette demande ne vous appartient pas");
        }
        if (demande.getStatut() != StatutConge.EN_ATTENTE) {
            throw new IllegalStateException("Seules les demandes en attente peuvent être annulées");
        }

        UserEntity user = demande.getUser();
        demande.setStatut(StatutConge.ANNULE);
        demande.setDateTraitement(LocalDateTime.now());
        DemandeConge saved = demandeCongeRepository.save(demande);
        
        log.info("Demande id={} annulée par userId={}", demandeId, userId);
        
        // Enregistrer l'annulation dans l'historique
        historyService.recordCancellation(user, saved, "Demande annulée par l'employé");
        
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<DemandeCongeResponse> getMesDemandes(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new EntityNotFoundException("Utilisateur introuvable");
        }
        return demandeCongeRepository.findByUserId(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<DemandeCongeResponse> getHistorique(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new EntityNotFoundException("Utilisateur introuvable");
        }
        return demandeCongeRepository.findByUserId(userId).stream()
                .filter(d -> STATUTS_HISTORIQUE.contains(d.getStatut()))
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SoldeCongeResponse> calculerSolde(Long userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Utilisateur introuvable"));

        int annee = Year.now().getValue();
        // Best effort inbound sync before read; local DB remains tracking/audit storage.
        dolibarrService.refreshAllocationsForUser(user, annee);
        List<EmployeeLeaveAllocation> allocations =
                employeeLeaveAllocationRepository.findAllAllocationsForYear(user, annee);

        Map<String, EmployeeLeaveAllocation> byCode = new java.util.HashMap<>();
        for (EmployeeLeaveAllocation row : allocations) {
            LeaveType leaveType = row.getLeaveType();
            if (leaveType != null && leaveType.getCode() != null) {
                byCode.put(leaveType.getCode().toUpperCase(java.util.Locale.ROOT), row);
            }
        }

        List<SoldeCongeResponse> result = new ArrayList<>();
        for (TypeConge type : TypeConge.values()) {
            EmployeeLeaveAllocation a = findAllocationByType(type, byCode);
            if (a != null) {
                result.add(SoldeCongeResponse.builder()
                        .typeConge(type)
                        .totalJours(a.getJoursInitiaux().intValue())
                        .joursPris(a.getJoursUtilises().intValue())
                        .joursRestants(a.getJoursDisponibles().intValue())
                        .build());
            } else {
                // Fallback: si pas d'allocation existante, renvoyer 0 (ou quota pays si tu préfères)
                result.add(SoldeCongeResponse.builder()
                        .typeConge(type)
                        .totalJours(0)
                        .joursPris(0)
                        .joursRestants(0)
                        .build());
            }
        }

        return result;
    }

    /**
     * Compte les jours ouvrables entre deux dates inclusives.
     * Exclut le samedi et le dimanche. Les jours fériés ne sont pas pris en compte pour l'instant
     * (évolution prévue avec la gestion multi-pays).
     */
    public int calculerJoursOuvrables(LocalDate debut, LocalDate fin) {
        return DemandeConge.calculerJoursOuvrables(debut, fin);
    }

    /**
     * Vérifie que l'utilisateur dispose d'assez de jours pour le type de congé demandé.
     * Les jours déjà pris ou réservés (demandes {@link StatutConge#ACCEPTE} ou {@link StatutConge#EN_ATTENTE})
     * sont déduits du plafond applicable.
     */
    @Transactional(readOnly = true)
    public void verifierSoldeDisponible(Long userId, TypeConge type, int joursDemandes) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Utilisateur introuvable"));
        if (joursDemandes <= 0) {
            throw new IllegalArgumentException("Le nombre de jours demandés doit être strictement positif");
        }

        switch (type) {
            case MALADIE, SANS_SOLDE -> {
                return;
            }
            case PAYE -> {
                int prisOuReserve = compterJoursPrisOuReservesPourType(userId, TypeConge.PAYE);
                int restants = countryPolicyService.getAnnualQuota(user.getPays(), TypeConge.PAYE) - prisOuReserve;
                if (joursDemandes > restants) {
                    throw new IllegalStateException(String.format(
                            "Solde de congés payés insuffisant : %d jour(s) ouvrable(s) demandé(s), %d disponible(s)",
                            joursDemandes,
                            Math.max(0, restants)));
                }
            }
            case COURTE_DUREE -> {
                int prisOuReserve = compterJoursPrisOuReservesPourType(userId, TypeConge.COURTE_DUREE);
                int quota = countryPolicyService.getAnnualQuota(user.getPays(), TypeConge.COURTE_DUREE);
                int restants = quota - prisOuReserve;
                if (joursDemandes > restants) {
                    throw new IllegalStateException(String.format(
                            "Solde congé courte durée insuffisant (plafond %d jours) : %d jour(s) demandé(s), %d disponible(s)",
                            quota,
                            joursDemandes,
                            Math.max(0, restants)));
                }
            }
        }
    }

    @Transactional
    public DemandeCongeResponse validerDemande(
            Long demandeId,
            Long rhId,
            boolean accepte,
            String commentaire
    ) {
        UserEntity rh = userRepository.findById(rhId)
                .orElseThrow(() -> new EntityNotFoundException("Utilisateur RH introuvable"));
        if (rh.getRole() != Role.RH && rh.getRole() != Role.MANAGER && rh.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Seul un validateur autorisé peut traiter une demande");
        }
        DemandeConge saved = workflowService.processDecision(
                demandeId,
                rh,
                accepte,
                StringUtils.hasText(commentaire) ? commentaire.trim() : null
        );
        log.info("Demande id={} {} par rhId={}", demandeId, accepte ? "acceptée" : "refusée", rhId);

        // Si la demande est définitivement acceptée, on met à jour Dolibarr (source de vérité)
        if (saved.getStatut() == StatutConge.ACCEPTE) {
            boolean synced = dolibarrService.syncApprovedLeave(saved);
            historyService.recordDolibarrSync(
                    saved.getUser(),
                    saved,
                    synced ? "ALLOCATION_UPDATED" : "ALLOCATION_UPDATE_FAILED"
            );
        }
        
        // Enregistrer la validation dans l'historique
        UserEntity employe = saved.getUser();
        if (accepte) {
            historyService.recordApproval(employe, saved, rh.getPrenom() + " " + rh.getNom());
        } else {
            historyService.recordRejection(employe, saved, commentaire != null ? commentaire : "Rejet sans motif spécifié");
        }
        
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<DemandeCongeResponse> getAllDemandesEnAttente() {
        return demandeCongeRepository.findByStatut(StatutConge.EN_ATTENTE).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public StatistiquesRhResponse getStatistiquesRh() {
        Map<StatutConge, Long> nombreParStatut = new EnumMap<>(StatutConge.class);
        for (StatutConge statut : StatutConge.values()) {
            nombreParStatut.put(statut, demandeCongeRepository.countByStatut(statut));
        }
        return StatistiquesRhResponse.builder()
                .nombreParStatut(nombreParStatut)
                .build();
    }

    private int compterJoursPrisOuReservesPourType(Long userId, TypeConge type) {
        List<JoursPrisParTypeProjection> rows = demandeCongeRepository.sumJoursPrisParTypePourUtilisateur(
                userId,
                STATUTS_COMPTABILISES_SOLDE
        );
        return rows.stream()
                .filter(r -> r.getTypeConge() == type)
                .findFirst()
                .map(r -> r.getTotalJours().intValue())
                .orElse(0);
    }

    private DemandeCongeResponse toResponse(DemandeConge d) {
        UserEntity u = d.getUser();
        return DemandeCongeResponse.builder()
                .id(d.getId())
                .typeConge(d.getTypeConge())
                .dateDebut(d.getDateDebut())
                .dateFin(d.getDateFin())
                .nombreJours(d.getNombreJours())
                .statut(d.getStatut())
                .motif(d.getMotif())
                .commentaireRh(d.getCommentaireRh())
                .dateSoumission(d.getDateSoumission())
                .dateTraitement(d.getDateTraitement())
                .employe(DemandeCongeResponse.EmployeInfo.builder()
                        .nom(u.getNom())
                        .prenom(u.getPrenom())
                        .email(u.getEmail())
                        .build())
                .build();
    }

    private EmployeeLeaveAllocation findAllocationByType(
            TypeConge typeConge,
            Map<String, EmployeeLeaveAllocation> byCode) {
        List<String> candidates = switch (typeConge) {
            case PAYE -> List.of("CONGES_PAYES", "PAID_LEAVE", "CP");
            case COURTE_DUREE -> List.of("RTT", "COURTE_DUREE", "SHORT_LEAVE");
            case MALADIE -> List.of("CONGE_MALADIE", "MALADIE", "SICK_LEAVE");
            case SANS_SOLDE -> List.of("CONGE_SANS_SOLDE", "SANS_SOLDE", "UNPAID_LEAVE");
        };
        for (String candidate : candidates) {
            EmployeeLeaveAllocation found = byCode.get(candidate);
            if (found != null) {
                return found;
            }
        }

        // Last fallback for custom code mappings: inspect local leave_type labels.
        return leaveTypeRepository.findByActiveTrue().stream()
                .filter(lt -> matchesTypeByLabel(typeConge, lt))
                .map(lt -> byCode.get(lt.getCode() == null ? null : lt.getCode().toUpperCase(java.util.Locale.ROOT)))
                .filter(java.util.Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    private boolean matchesTypeByLabel(TypeConge typeConge, LeaveType leaveType) {
        String raw = (leaveType.getLibelle() + " " + leaveType.getCode()).toLowerCase(java.util.Locale.ROOT);
        return switch (typeConge) {
            case PAYE -> raw.contains("pay") || raw.contains("cp");
            case COURTE_DUREE -> raw.contains("rtt") || raw.contains("courte");
            case MALADIE -> raw.contains("malad") || raw.contains("sick");
            case SANS_SOLDE -> raw.contains("sans solde") || raw.contains("unpaid");
        };
    }
}
