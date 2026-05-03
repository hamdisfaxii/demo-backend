package com.example.conges.service;

import com.example.conges.dto.dolibarr.DolibarrEmployeeDto;
import com.example.conges.dto.dolibarr.DolibarrHolidayDto;
import com.example.conges.dto.dolibarr.DolibarrLeaveAllocationDto;
import com.example.conges.dto.dolibarr.DolibarrLeaveTypeDto;
import com.example.conges.entity.DemandeConge;
import com.example.conges.entity.EmployeeLeaveAllocation;
import com.example.conges.entity.LeaveType;
import com.example.conges.entity.Role;
import com.example.conges.entity.SyncDirection;
import com.example.conges.entity.TypeConge;
import com.example.conges.entity.UserEntity;
import com.example.conges.repository.EmployeeLeaveAllocationRepository;
import com.example.conges.repository.LeaveTypeRepository;
import com.example.conges.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * Service pour intégrer l'API Dolibarr
 * Récupère les employés depuis Dolibarr et les synchronise avec la base de données locale
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DolibarrService {

    private static final int OUTBOUND_MAX_RETRIES = 3;

    private static final Map<String, String> ISO3166_ALPHA3_TO2 =
            Map.of("FRA", "FR", "TUN", "TN", "MAR", "MA");
    private static final Map<Integer, String> NUMERIC_ISO_TO_ALPHA2 =
            Map.of(250, "FR", 788, "TN", 504, "MA");
    private static final Set<String> FR_OVERSEAS_ISO2 =
            Set.of(
                    "GP", "MQ", "GF", "RE", "YT", "PM", "BL",
                    "MF", "WF", "PF", "NC", "TF"
            );

    private final UserRepository userRepository;
    private final EmployeeLeaveAllocationRepository employeeLeaveAllocationRepository;
    private final LeaveTypeRepository leaveTypeRepository;
    private final DolibarrSyncLogService dolibarrSyncLogService;
    private final JdbcTemplate jdbcTemplate;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${dolibarr.database.table-prefix:llx_}")
    private String dolibarrTablePrefix;

    @Value("${dolibarr.url:http://localhost/dolibarr/htdocs/api/index.php}")
    private String dolibarrUrl;

    @Value("${dolibarr.api-key:}")
    private String dolibarrApiKey;

    /**
     * Récupère la liste des employés depuis Dolibarr
     *
     * @return Liste des employés Dolibarr actifs
     */
    public List<DolibarrEmployeeDto> getEmployeesFromDolibarr() {
        if (!isDolibarrConfigured()) {
            log.warn("Dolibarr n'est pas configuré. Vérifiez dolibarr.url et dolibarr.api-key");
            return new ArrayList<>();
        }

        try {
            String url = dolibarrUrl + "/users?sortfield=rowid&sortorder=ASC&limit=100";
            HttpHeaders headers = createHeaders();
            HttpEntity<?> entity = new HttpEntity<>(headers);

            ResponseEntity<DolibarrEmployeeDto[]> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    DolibarrEmployeeDto[].class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                log.info("Récupération de {} employés depuis Dolibarr", response.getBody().length);
                return List.of(response.getBody());
            }

            log.error("Erreur lors de la récupération des employés Dolibarr: {}", 
                     response.getStatusCode());
            return new ArrayList<>();

        } catch (RestClientException e) {
            log.error("Erreur de connexion à Dolibarr: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Récupère détail d'un employé de Dolibarr
     */
    public DolibarrEmployeeDto getEmployeeFromDolibarr(Long employeeId) {
        if (!isDolibarrConfigured()) {
            log.warn("Dolibarr n'est pas configuré");
            return null;
        }

        try {
            String url = dolibarrUrl + "/users/" + employeeId;
            HttpHeaders headers = createHeaders();
            HttpEntity<?> entity = new HttpEntity<>(headers);

            ResponseEntity<DolibarrEmployeeDto> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    DolibarrEmployeeDto.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Employé {} récupéré depuis Dolibarr", employeeId);
                return response.getBody();
            }

            log.error("Employé {} non trouvé dans Dolibarr", employeeId);
            return null;

        } catch (RestClientException e) {
            log.error("Erreur lors de la récupération de l'employé {} : {}", employeeId, e.getMessage());
            return null;
        }
    }

    /**
     * Synchronise les employés Dolibarr avec la base de données
     */
    @Transactional
    public int syncEmployeesFromDolibarr() {
        List<DolibarrEmployeeDto> dolibarrEmployees = getEmployeesFromDolibarr();

        if (dolibarrEmployees.isEmpty()) {
            log.warn("Aucun employé trouvé sur Dolibarr");
            return 0;
        }

        int syncCount = 0;

        for (DolibarrEmployeeDto doliEmployee : dolibarrEmployees) {
            // Filtre les employés inactifs
            if (!doliEmployee.isActive()) {
                log.debug("Employé {} est inactif, ignoré", doliEmployee.getEmail());
                continue;
            }

            // Vérifie si l'employé existe déjà en local
            UserEntity existingUser = userRepository.findByDolibarrId(doliEmployee.getId())
                    .orElse(null);

            if (existingUser != null) {
                // Mise à jour de l'employé existant
                existingUser.setNom(doliEmployee.getLastName());
                existingUser.setPrenom(doliEmployee.getFirstName());
                existingUser.setEmail(doliEmployee.getEmail());
                existingUser.setPays(
                        normalizeToSupportedHrPays(
                                resolveCountryCode(doliEmployee.getId(), doliEmployee)));
                userRepository.save(existingUser);
                dolibarrSyncLogService.logSuccess(
                        "USER",
                        "UPSERT",
                        existingUser.getId(),
                        doliEmployee.getId(),
                        SyncDirection.INBOUND,
                        doliEmployee
                );
                log.debug("Employé {} mis à jour", doliEmployee.getEmail());
                syncCount++;
            } else {
                // Création d'un nouvel employé
                UserEntity newUser = UserEntity.builder()
                        .dolibarrId(doliEmployee.getId())
                        .email(doliEmployee.getEmail())
                        .nom(doliEmployee.getLastName())
                        .prenom(doliEmployee.getFirstName())
                        .pays(normalizeToSupportedHrPays(
                                resolveCountryCode(doliEmployee.getId(), doliEmployee)))
                        .role(Role.EMPLOYE)  // Par défaut, tout nouvel employé est EMPLOYE
                        .build();

                userRepository.save(newUser);
                dolibarrSyncLogService.logSuccess(
                        "USER",
                        "UPSERT",
                        newUser.getId(),
                        doliEmployee.getId(),
                        SyncDirection.INBOUND,
                        doliEmployee
                );
                log.info("Nouvel employé créé: {} {}", 
                        doliEmployee.getFirstName(), doliEmployee.getLastName());
                syncCount++;
            }
        }

        log.info("Synchronisation Dolibarr complétée: {} employés synchronisés", syncCount);
        return syncCount;
    }

    /**
     * Récupère un employé par son email Dolibarr et le synchronise
     */
    @Transactional
    public UserEntity syncEmployeeByEmail(String email) {
        // D'abord, vérifier si l'employé existe déjà localement
        UserEntity existingUser = userRepository.findByEmail(email)
                .orElse(null);

        if (existingUser != null && existingUser.getDolibarrId() != null) {
            log.debug("Employé {} existe déjà localement avec ID Dolibarr {}", 
                     email, existingUser.getDolibarrId());
            return existingUser;
        }

        // Récupérer tous les employés de Dolibarr et chercher celui avec cet email
        List<DolibarrEmployeeDto> employees = getEmployeesFromDolibarr();
        DolibarrEmployeeDto foundEmployee = employees.stream()
                .filter(emp -> emp.getEmail() != null && emp.getEmail().equals(email))
                .findFirst()
                .orElse(null);

        if (foundEmployee == null) {
            log.warn("Employé avec email {} non trouvé sur Dolibarr", email);
            return null;
        }

        // Synchroniser l'employé trouvé
        if (existingUser != null) {
            existingUser.setDolibarrId(foundEmployee.getId());
            existingUser.setNom(foundEmployee.getLastName());
            existingUser.setPrenom(foundEmployee.getFirstName());
            existingUser.setPays(normalizeToSupportedHrPays(
                    resolveCountryCode(foundEmployee.getId(), foundEmployee)));
            userRepository.save(existingUser);
            log.info("Employé {} lié à Dolibarr (ID: {})", email, foundEmployee.getId());
            return existingUser;
        } else {
            UserEntity newUser = UserEntity.builder()
                    .dolibarrId(foundEmployee.getId())
                    .email(email)
                    .nom(foundEmployee.getLastName())
                    .prenom(foundEmployee.getFirstName())
                    .pays(normalizeToSupportedHrPays(
                            resolveCountryCode(foundEmployee.getId(), foundEmployee)))
                    .role(Role.EMPLOYE)
                    .build();
            userRepository.save(newUser);
            log.info("Nouvel employé créé et synchronisé avec Dolibarr: {}", email);
            return newUser;
        }
    }

    /**
     * Récupère la liste des types de congés depuis Dolibarr
     */
    public List<DolibarrLeaveTypeDto> getLeaveTypesFromDolibarr() {
        if (!isDolibarrConfigured()) {
            log.warn("Dolibarr n'est pas configuré");
            return new ArrayList<>();
        }

        try {
            // Note: Le endpoint varie selon la version Dolibarr
            // Peut être: /leavetypes, /holidays/types, /hrm/leavetypes
            String url = dolibarrUrl + "/leavetypes?sortfield=rowid&sortorder=ASC&limit=100";
            HttpHeaders headers = createHeaders();
            HttpEntity<?> entity = new HttpEntity<>(headers);

            ResponseEntity<DolibarrLeaveTypeDto[]> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    DolibarrLeaveTypeDto[].class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                log.info("Récupération de {} types de congés depuis Dolibarr", response.getBody().length);
                return List.of(response.getBody());
            }

            log.warn("Aucun type de congé trouvé sur Dolibarr");
            return new ArrayList<>();

        } catch (RestClientException e) {
            log.error("Erreur lors de la récupération des types de congés: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Récupère la liste des jours fériés depuis Dolibarr
     */
    public List<DolibarrHolidayDto> getHolidaysFromDolibarr() {
        if (!isDolibarrConfigured()) {
            log.warn("Dolibarr n'est pas configuré");
            return new ArrayList<>();
        }

        try {
            // Note: Le endpoint varie selon la version Dolibarr
            // Peut être: /holidays, /bank_holidays, /leaves/holidays
            String url = dolibarrUrl + "/holidays?sortfield=rowid&sortorder=ASC&limit=500";
            HttpHeaders headers = createHeaders();
            HttpEntity<?> entity = new HttpEntity<>(headers);

            ResponseEntity<DolibarrHolidayDto[]> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    DolibarrHolidayDto[].class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                log.info("Récupération de {} jours fériés depuis Dolibarr", response.getBody().length);
                return List.of(response.getBody());
            }

            log.warn("Aucun jour férié trouvé sur Dolibarr");
            return new ArrayList<>();

        } catch (RestClientException e) {
            log.error("Erreur lors de la récupération des jours fériés: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Récupère les allocations de congés par employé depuis Dolibarr
     */
    public List<DolibarrLeaveAllocationDto> getLeaveAllocationsFromDolibarr() {
        if (!isDolibarrConfigured()) {
            log.warn("Dolibarr n'est pas configuré");
            return new ArrayList<>();
        }

        try {
            // Note: Le endpoint varie selon la version Dolibarr
            // Peut être: /leaves/allocations, /hrm/allocations, /leaves
            String url = dolibarrUrl + "/leaves/allocations?sortfield=rowid&sortorder=ASC&limit=500";
            HttpHeaders headers = createHeaders();
            HttpEntity<?> entity = new HttpEntity<>(headers);

            ResponseEntity<DolibarrLeaveAllocationDto[]> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    DolibarrLeaveAllocationDto[].class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                log.info("Récupération de {} allocations de congés depuis Dolibarr", response.getBody().length);
                return List.of(response.getBody());
            }

            log.warn("Aucune allocation de congé trouvée sur Dolibarr");
            return new ArrayList<>();

        } catch (RestClientException e) {
            log.error("Erreur lors de la récupération des allocations de congés: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Teste la connexion à Dolibarr
     */
    public boolean testDolibarrConnection() {
        if (!isDolibarrConfigured()) {
            return false;
        }

        try {
            String url = dolibarrUrl + "/users?limit=1";
            HttpHeaders headers = createHeaders();
            HttpEntity<?> entity = new HttpEntity<>(headers);

            ResponseEntity<DolibarrEmployeeDto[]> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    DolibarrEmployeeDto[].class
            );

            boolean success = response.getStatusCode().is2xxSuccessful();
            log.info("Test de connexion Dolibarr: {}", success ? "✅ Succès" : "❌ Échoué");
            return success;

        } catch (RestClientException e) {
            log.error("Erreur lors du test de connexion Dolibarr: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Synchronisation sortante: envoie une demande locale vers Dolibarr.
     */
    public Long pushLeaveRequest(DemandeConge demande) {
        if (!isDolibarrConfigured()) {
            dolibarrSyncLogService.logFailure(
                    "LEAVE_REQUEST",
                    "CREATE",
                    demande.getId(),
                    null,
                    SyncDirection.OUTBOUND,
                    "Dolibarr non configuré",
                    null
            );
            return null;
        }
        try {
            String url = dolibarrUrl + "/leaves";
            HttpHeaders headers = createHeaders();
            Map<String, Object> payload = new HashMap<>();
            payload.put("fk_user", demande.getUser().getDolibarrId());
            payload.put("date_debut", demande.getDateDebut().toString());
            payload.put("date_fin", demande.getDateFin().toString());
            payload.put("type_code", demande.getTypeConge().name());
            payload.put("note", demande.getMotif());
            payload.put("status", demande.getStatut().name());

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null && response.getBody().get("id") != null) {
                Long remoteId = Long.valueOf(response.getBody().get("id").toString());
                dolibarrSyncLogService.logSuccess(
                        "LEAVE_REQUEST",
                        "CREATE",
                        demande.getId(),
                        remoteId,
                        SyncDirection.OUTBOUND,
                        payload
                );
                return remoteId;
            }
            dolibarrSyncLogService.logFailure(
                    "LEAVE_REQUEST",
                    "CREATE",
                    demande.getId(),
                    null,
                    SyncDirection.OUTBOUND,
                    "Réponse Dolibarr invalide",
                    response.getBody()
            );
            return null;
        } catch (Exception ex) {
            dolibarrSyncLogService.logFailure(
                    "LEAVE_REQUEST",
                    "CREATE",
                    demande.getId(),
                    null,
                    SyncDirection.OUTBOUND,
                    ex.getMessage(),
                    null
            );
            return null;
        }
    }

    public DolibarrEmployeeDto authenticateUserViaApi(String email, String password) {
        if (!isDolibarrConfigured()) {
            return null;
        }
        Map<String, Object> payload = new HashMap<>();
        payload.put("login", email);
        payload.put("password", password);

        List<String> candidateEndpoints = List.of("/login", "/auth/login");
        boolean loginSucceeded = false;
        for (String endpoint : candidateEndpoints) {
            try {
                String url = dolibarrUrl + endpoint;
                HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, createHeaders());
                ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);
                if (response.getStatusCode().is2xxSuccessful()) {
                    loginSucceeded = true;
                    break;
                }
            } catch (Exception ex) {
                log.debug("Dolibarr login endpoint {} indisponible: {}", endpoint, ex.getMessage());
            }
        }
        if (!loginSucceeded) {
            return null;
        }

        return getEmployeesFromDolibarr().stream()
                .filter(emp -> emp.getEmail() != null && emp.getEmail().equalsIgnoreCase(email))
                .findFirst()
                .orElse(null);
    }

    /**
     * Outbound sync when a workflow completes approval.
     * Keeps Dolibarr as source-of-truth: status + allocation.
     */
    public boolean syncApprovedLeave(DemandeConge demande) {
        if (demande == null || demande.getUser() == null) {
            return false;
        }
        boolean leaveUpdated = true;
        if (demande.getDolibarrLeaveRequestId() != null) {
            leaveUpdated = updateLeaveStatus(demande.getDolibarrLeaveRequestId(), "APPROVED", demande);
        }
        boolean allocationUpdated = consumeAllocationForApprovedLeave(demande);
        if (allocationUpdated) {
            refreshAllocationsForUser(demande.getUser(), demande.getDateDebut().getYear());
        }
        return leaveUpdated && allocationUpdated;
    }

    public void refreshAllocationsForUser(UserEntity user, int year) {
        if (user == null || user.getDolibarrId() == null || !isDolibarrConfigured()) {
            return;
        }
        List<DolibarrLeaveAllocationDto> remoteAllocations = getLeaveAllocationsFromDolibarr().stream()
                .filter(DolibarrLeaveAllocationDto::isActive)
                .filter(a -> user.getDolibarrId().equals(a.getEmployeeId()))
                .filter(a -> a.getAnnee() == null || a.getAnnee().equals(year))
                .toList();

        for (DolibarrLeaveAllocationDto remote : remoteAllocations) {
            Optional<LeaveType> leaveTypeOpt = leaveTypeRepository.findByDolibarrLeaveTypeId(remote.getTypeCongeId());
            if (leaveTypeOpt.isEmpty()) {
                continue;
            }
            LeaveType leaveType = leaveTypeOpt.get();
            Optional<EmployeeLeaveAllocation> existing = employeeLeaveAllocationRepository.findByDolibarrAllocationId(remote.getId());
            if (existing.isPresent()) {
                EmployeeLeaveAllocation allocation = existing.get();
                allocation.setJoursInitiaux(remote.getJoursInitiaux());
                allocation.setJoursUtilises(remote.getJoursUtilises());
                allocation.setJoursDisponibles(remote.getJoursDisponibles());
                allocation.setAnnee(remote.getAnnee() == null ? year : remote.getAnnee());
                allocation.setDateDebut(remote.getDateDebut());
                allocation.setDateFin(remote.getDateFin());
                allocation.setActive(remote.isActive());
                allocation.setUpdatedAt(LocalDateTime.now());
                employeeLeaveAllocationRepository.save(allocation);
            } else {
                EmployeeLeaveAllocation newAllocation = EmployeeLeaveAllocation.builder()
                        .employee(user)
                        .leaveType(leaveType)
                        .dolibarrAllocationId(remote.getId())
                        .joursInitiaux(remote.getJoursInitiaux())
                        .joursUtilises(remote.getJoursUtilises())
                        .joursDisponibles(remote.getJoursDisponibles())
                        .annee(remote.getAnnee() == null ? year : remote.getAnnee())
                        .dateDebut(remote.getDateDebut())
                        .dateFin(remote.getDateFin())
                        .active(remote.isActive())
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build();
                employeeLeaveAllocationRepository.save(newAllocation);
            }
        }
    }

    private boolean updateLeaveStatus(Long remoteLeaveId, String status, DemandeConge demande) {
        Map<String, Object> payload = Map.of("status", status);
        return callWithRetry("LEAVE_REQUEST", "UPDATE_STATUS", demande.getId(), remoteLeaveId, payload, () -> {
            String url = dolibarrUrl + "/leaves/" + remoteLeaveId;
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, createHeaders());
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.PUT, entity, Map.class);
            return response.getStatusCode().is2xxSuccessful();
        });
    }

    private boolean consumeAllocationForApprovedLeave(DemandeConge demande) {
        if (demande.getUser().getDolibarrId() == null) {
            return false;
        }
        int year = demande.getDateDebut() == null ? java.time.Year.now().getValue() : demande.getDateDebut().getYear();
        refreshAllocationsForUser(demande.getUser(), year);

        Optional<EmployeeLeaveAllocation> allocationOpt = findAllocationForType(
                demande.getUser(),
                demande.getTypeConge(),
                year
        );
        if (allocationOpt.isEmpty()) {
            dolibarrSyncLogService.logFailure(
                    "LEAVE_ALLOCATION",
                    "CONSUME",
                    demande.getId(),
                    null,
                    SyncDirection.OUTBOUND,
                    "Allocation introuvable pour type " + demande.getTypeConge(),
                    null
            );
            return false;
        }

        EmployeeLeaveAllocation allocation = allocationOpt.get();
        double nextUsed = (allocation.getJoursUtilises() == null ? 0D : allocation.getJoursUtilises()) + demande.getNombreJours();
        double nextAvailable = (allocation.getJoursDisponibles() == null ? 0D : allocation.getJoursDisponibles()) - demande.getNombreJours();
        if (nextAvailable < 0) {
            dolibarrSyncLogService.logFailure(
                    "LEAVE_ALLOCATION",
                    "CONSUME",
                    demande.getId(),
                    allocation.getDolibarrAllocationId(),
                    SyncDirection.OUTBOUND,
                    "Solde Dolibarr insuffisant",
                    Map.of("requestedDays", demande.getNombreJours(), "available", allocation.getJoursDisponibles())
            );
            return false;
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("qty_used", nextUsed);
        payload.put("qty_available", nextAvailable);

        boolean updated = callWithRetry(
                "LEAVE_ALLOCATION",
                "CONSUME",
                demande.getId(),
                allocation.getDolibarrAllocationId(),
                payload,
                () -> {
                    String url = dolibarrUrl + "/leaves/allocations/" + allocation.getDolibarrAllocationId();
                    HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, createHeaders());
                    ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.PUT, entity, Map.class);
                    return response.getStatusCode().is2xxSuccessful();
                }
        );

        if (updated) {
            allocation.setJoursUtilises(nextUsed);
            allocation.setJoursDisponibles(nextAvailable);
            allocation.setUpdatedAt(LocalDateTime.now());
            employeeLeaveAllocationRepository.save(allocation);
        }

        return updated;
    }

    private Optional<EmployeeLeaveAllocation> findAllocationForType(UserEntity user, TypeConge typeConge, int year) {
        List<LeaveType> activeTypes = leaveTypeRepository.findByActiveTrue();
        List<LeaveType> candidates = activeTypes.stream()
                .filter(type -> mapTypeConge(typeConge, type))
                .toList();
        for (LeaveType leaveType : candidates) {
            Optional<EmployeeLeaveAllocation> allocation = employeeLeaveAllocationRepository
                    .findByEmployeeAndLeaveTypeAndAnneeAndActiveTrue(user, leaveType, year);
            if (allocation.isPresent()) {
                return allocation;
            }
        }
        return Optional.empty();
    }

    private boolean mapTypeConge(TypeConge typeConge, LeaveType leaveType) {
        String code = leaveType.getCode() == null ? "" : leaveType.getCode().toUpperCase(java.util.Locale.ROOT);
        String label = leaveType.getLibelle() == null ? "" : leaveType.getLibelle().toLowerCase(java.util.Locale.ROOT);
        return switch (typeConge) {
            case PAYE -> code.contains("CONGES_PAYES") || code.equals("CP") || label.contains("pay");
            case COURTE_DUREE -> code.contains("RTT") || label.contains("courte");
            case MALADIE -> code.contains("MALADIE") || label.contains("malad");
            case SANS_SOLDE -> code.contains("SANS_SOLDE") || label.contains("sans solde");
        };
    }

    private boolean callWithRetry(
            String entityType,
            String operation,
            Long localEntityId,
            Long remoteEntityId,
            Object payload,
            java.util.function.Supplier<Boolean> call
    ) {
        Exception lastError = null;
        for (int attempt = 1; attempt <= OUTBOUND_MAX_RETRIES; attempt++) {
            try {
                boolean ok = Boolean.TRUE.equals(call.get());
                if (ok) {
                    dolibarrSyncLogService.logSuccess(
                            entityType,
                            operation,
                            localEntityId,
                            remoteEntityId,
                            SyncDirection.OUTBOUND,
                            payload
                    );
                    return true;
                }
            } catch (Exception ex) {
                lastError = ex;
                log.warn("Dolibarr outbound {} tentative {}/{} échouée: {}",
                        operation, attempt, OUTBOUND_MAX_RETRIES, ex.getMessage());
            }
            try {
                Thread.sleep(300L * attempt);
            } catch (InterruptedException interruptedException) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        dolibarrSyncLogService.logFailure(
                entityType,
                operation,
                localEntityId,
                remoteEntityId,
                SyncDirection.OUTBOUND,
                lastError == null ? "Échec API Dolibarr" : lastError.getMessage(),
                payload
        );
        return false;
    }

    /** TN | FR | MA si reconnu après normalisation DOM ; sinon TN. */
    public String normalizeToSupportedHrPays(String isoGuess) {
        if (isoGuess == null || isoGuess.isBlank()) {
            return "TN";
        }
        String u = isoGuess.trim().toUpperCase(Locale.ROOT);
        u = mapFrenchOverseasToMetro(u);
        if ("TN".equals(u) || "FR".equals(u) || "MA".equals(u)) {
            return u;
        }
        return "TN";
    }

    /** À la connexion et pour la synchro employés depuis Dolibarr. */
    public String resolveSupportedHrCountryIso2(Long dolibarrRowId, DolibarrEmployeeDto dto) {
        return normalizeToSupportedHrPays(resolveCountryCode(dolibarrRowId, dto));
    }

    /**
     * Priorité jointure utilisateur Dolibarr + table pays puis champs pays de la réponse API.
     *
     * @return Alpha-2 (DOM français normalisées en FR métropole métier), ou {@code null}
     */
    public String resolveCountryCode(Long dolibarrUserRowId, DolibarrEmployeeDto dto) {
        String fromSql = iso2FromJoinRowJdbc(dolibarrUserRowId);
        if (fromSql != null && !fromSql.isBlank()) {
            return mapFrenchOverseasToMetro(fromSql.trim().toUpperCase(Locale.ROOT));
        }
        if (dto != null && dto.getCountryCode() != null && !dto.getCountryCode().isBlank()) {
            return iso2FromMixedApi(dto.getCountryCode().trim());
        }
        return null;
    }

    private String iso2FromJoinRowJdbc(Long userRowId) {
        if (userRowId == null) {
            return null;
        }
        String tblUser = qualifiedDolibarrTableName("user");
        String tblCountry = qualifiedDolibarrTableName("c_country");
        String sql = "SELECT c.code AS c_code, c.code_iso AS c_code_iso, c.label AS c_label "
                + "FROM `" + tblUser + "` u "
                + "LEFT JOIN `" + tblCountry + "` c ON c.rowid = u.fk_country "
                + "WHERE u.rowid = ? LIMIT 1";
        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, userRowId);
            if (rows.isEmpty()) {
                return null;
            }
            return rowToIso2Guess(rows.get(0));
        } catch (Exception ex) {
            log.debug("Lecture fk_country Dolibarr (SQL): {}", ex.getMessage());
            return null;
        }
    }

    private String qualifiedDolibarrTableName(String logicalSuffixSansLlPrefix) {
        String prefix = dolibarrTablePrefix == null ? "llx_" : dolibarrTablePrefix.trim().replace("`", "");
        prefix = prefix.replaceAll("[^a-zA-Z0-9_]", "");
        if (prefix.isEmpty()) {
            prefix = "llx";
        }
        if (!prefix.endsWith("_")) {
            prefix += "_";
        }
        String base = logicalSuffixSansLlPrefix == null ? ""
                : logicalSuffixSansLlPrefix.replaceFirst("(?i)^llx_", "").replaceAll("[^a-zA-Z0-9_]", "");
        return prefix + base;
    }

    private static String rowToIso2Guess(Map<String, Object> row) {
        String codeIso = toTrimmedString(row.get("c_code_iso"));
        String code = toTrimmedString(row.get("c_code"));
        String raw = firstNonBlank(codeIso, code);
        String guessed = iso2FromMixedApi(raw);
        if (guessed == null || guessed.isBlank()) {
            guessed = iso2FromLabel(toTrimmedString(row.get("c_label")));
        }
        return guessed;
    }

    private static String toTrimmedString(Object o) {
        if (o == null) {
            return null;
        }
        String s = o.toString().trim();
        return s.isEmpty() ? null : s;
    }

    private static String firstNonBlank(String a, String b) {
        if (a != null && !a.isBlank()) {
            return a;
        }
        return (b != null && !b.isBlank()) ? b : null;
    }

    private static String iso2FromMixedApi(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String s = raw.trim().toUpperCase(Locale.ROOT).replaceAll("\\s+", "");
        if (s.matches("\\d{1,3}")) {
            try {
                int n = Integer.parseInt(s);
                String r = NUMERIC_ISO_TO_ALPHA2.get(n);
                return r != null ? mapFrenchOverseasToMetro(r) : null;
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        if (s.length() == 2 && s.matches("[A-Z]{2}")) {
            return mapFrenchOverseasToMetro(s);
        }
        if (s.length() == 3) {
            String r = ISO3166_ALPHA3_TO2.get(s);
            return r != null ? mapFrenchOverseasToMetro(r) : null;
        }
        return null;
    }

    private static String iso2FromLabel(String label) {
        if (label == null || label.isBlank()) {
            return null;
        }
        String t = label.toLowerCase(Locale.ROOT);
        if (t.contains("tunis")) {
            return "TN";
        }
        if (t.contains("maroc")) {
            return "MA";
        }
        if (t.contains("franc") || t.contains("france")) {
            return "FR";
        }
        return null;
    }

    /** Outre-mer français → même socle quotas que métropole. */
    private static String mapFrenchOverseasToMetro(String iso2) {
        if (iso2 == null || iso2.isBlank()) {
            return iso2;
        }
        String u = iso2.trim().toUpperCase(Locale.ROOT);
        if (FR_OVERSEAS_ISO2.contains(u)) {
            return "FR";
        }
        return u;
    }

    /**
     * Vérifie si Dolibarr est configuré
     */
    private boolean isDolibarrConfigured() {
        return dolibarrUrl != null && !dolibarrUrl.isEmpty() &&
               dolibarrApiKey != null && !dolibarrApiKey.isEmpty();
    }

    /**
     * Crée les headers HTTP avec la clé API Dolibarr
     */
    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("DOLAPIKEY", dolibarrApiKey);
        headers.set("Accept", "application/json");
        headers.set("Content-Type", "application/json");
        return headers;
    }

    /**
     * Récupère le statut de la configuration Dolibarr
     */
    public String getDolibarrStatus() {
        if (!isDolibarrConfigured()) {
            return "❌ Non configuré (manque URL ou clé API)";
        }

        boolean connected = testDolibarrConnection();
        if (connected) {
            return "✅ Connecté à Dolibarr";
        } else {
            return "❌ Erreur de connexion à Dolibarr";
        }
    }
}
