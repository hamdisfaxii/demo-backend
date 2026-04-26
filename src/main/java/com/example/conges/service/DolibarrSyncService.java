package com.example.conges.service;

import com.example.conges.dto.dolibarr.DolibarrHolidayDto;
import com.example.conges.dto.dolibarr.DolibarrLeaveAllocationDto;
import com.example.conges.dto.dolibarr.DolibarrLeaveTypeDto;
import com.example.conges.entity.EmployeeLeaveAllocation;
import com.example.conges.entity.Holiday;
import com.example.conges.entity.LeaveType;
import com.example.conges.entity.SyncDirection;
import com.example.conges.entity.UserEntity;
import com.example.conges.repository.EmployeeLeaveAllocationRepository;
import com.example.conges.repository.HolidayRepository;
import com.example.conges.repository.LeaveTypeRepository;
import com.example.conges.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service pour synchroniser TOUTES les données depuis Dolibarr
 * Inclut : employés, types de congés, jours fériés, allocations de congés
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DolibarrSyncService {

    private final DolibarrService dolibarrService;
    private final UserRepository userRepository;
    private final LeaveTypeRepository leaveTypeRepository;
    private final HolidayRepository holidayRepository;
    private final EmployeeLeaveAllocationRepository allocationRepository;
    private final DolibarrSyncLogService dolibarrSyncLogService;

    /**
     * Synchronise TOUTES les données depuis Dolibarr
     * - Employés
     * - Types de congés
     * - Jours fériés
     * - Allocations de congés
     *
     * @return Rapport complet de la synchronisation
     */
    @Transactional
    public DolibarrFullSyncResponse syncAllDataFromDolibarr() {
        log.info("🔄 Début synchronisation COMPLÈTE depuis Dolibarr...");

        DolibarrFullSyncResponse response = new DolibarrFullSyncResponse();

        try {
            // 1. Synchroniser les employés
            int employeesSynced = dolibarrService.syncEmployeesFromDolibarr();
            response.setEmployeesSynced(employeesSynced);
            log.info("✅ {} employés synchronisés", employeesSynced);

            // 2. Synchroniser les types de congés
            int leaveTypesSynced = syncLeaveTypes();
            response.setLeaveTypesSynced(leaveTypesSynced);
            log.info("✅ {} types de congés synchronisés", leaveTypesSynced);

            // 3. Synchroniser les jours fériés
            int holidaysSynced = syncHolidays();
            response.setHolidaysSynced(holidaysSynced);
            log.info("✅ {} jours fériés synchronisés", holidaysSynced);

            // 4. Synchroniser les allocations de congés
            int allocationsSynced = syncLeaveAllocations();
            response.setAllocationsSynced(allocationsSynced);
            log.info("✅ {} allocations de congés synchronisées", allocationsSynced);

            response.setSuccess(true);
            response.setMessage("Synchronisation COMPLÈTE réussie !");

        } catch (Exception e) {
            log.error("❌ Erreur lors de la synchronisation: {}", e.getMessage(), e);
            response.setSuccess(false);
            response.setMessage("Erreur: " + e.getMessage());
        }

        log.info("🎉 Synchronisation terminée: {}", response.getMessage());
        return response;
    }

    /**
     * Synchronise les types de congés depuis Dolibarr
     */
    @Transactional
    public int syncLeaveTypes() {
        log.info("📋 Synchronisation des types de congés...");

        List<DolibarrLeaveTypeDto> dolibarrTypes = dolibarrService.getLeaveTypesFromDolibarr();
        int syncCount = 0;

        for (DolibarrLeaveTypeDto doliType : dolibarrTypes) {
            if (!doliType.isActive()) {
                log.debug("Skipping inactive leave type: {}", doliType.getCode());
                continue;
            }

            Optional<LeaveType> existing = leaveTypeRepository.findByDolibarrLeaveTypeId(doliType.getId());

            if (existing.isPresent()) {
                // Mise à jour
                LeaveType leaveType = existing.get();
                leaveType.setCode(doliType.getCode());
                leaveType.setLibelle(doliType.getLibelle());
                leaveType.setDescription(doliType.getDescription());
                leaveType.setCouleur(doliType.getCouleur());
                leaveType.setActive(doliType.isActive());
                leaveType.setRequiresApproval(doliType.requiresApproval());
                leaveType.setDelai(doliType.getDelai() != null ? doliType.getDelai().longValue() : 0L);
                leaveType.setUpdatedAt(LocalDateTime.now());

                leaveTypeRepository.save(leaveType);
                dolibarrSyncLogService.logSuccess("LEAVE_TYPE", "UPSERT", leaveType.getId(), doliType.getId(), SyncDirection.INBOUND, doliType);
                log.debug("✏️ Type de congé mis à jour: {}", doliType.getCode());
            } else {
                // Création
                LeaveType newType = LeaveType.builder()
                        .dolibarrLeaveTypeId(doliType.getId())
                        .code(doliType.getCode())
                        .libelle(doliType.getLibelle())
                        .description(doliType.getDescription())
                        .couleur(doliType.getCouleur())
                        .active(doliType.isActive())
                        .requiresApproval(doliType.requiresApproval())
                        .delai(doliType.getDelai() != null ? doliType.getDelai().longValue() : 0L)
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build();

                leaveTypeRepository.save(newType);
                dolibarrSyncLogService.logSuccess("LEAVE_TYPE", "UPSERT", newType.getId(), doliType.getId(), SyncDirection.INBOUND, doliType);
                log.debug("✨ Nouveau type de congé créé: {}", doliType.getCode());
            }

            syncCount++;
        }

        log.info("📋 {} types de congés synchronisés", syncCount);
        return syncCount;
    }

    /**
     * Synchronise les jours fériés depuis Dolibarr
     */
    @Transactional
    public int syncHolidays() {
        log.info("🎉 Synchronisation des jours fériés...");

        List<DolibarrHolidayDto> dolibarrHolidays = dolibarrService.getHolidaysFromDolibarr();
        int syncCount = 0;

        for (DolibarrHolidayDto doliHoliday : dolibarrHolidays) {
            if (!doliHoliday.isActive()) {
                log.debug("Skipping inactive holiday: {}", doliHoliday.getLibelle());
                continue;
            }

            Optional<Holiday> existing = holidayRepository.findByDolibarrHolidayId(doliHoliday.getId());

            if (existing.isPresent()) {
                // Mise à jour
                Holiday holiday = existing.get();
                holiday.setLibelle(doliHoliday.getLibelle());
                holiday.setDateJour(doliHoliday.getDateJour());
                holiday.setDuree(doliHoliday.getDuree() != null ? doliHoliday.getDuree() : 1.0);
                holiday.setIdPays(doliHoliday.getIdPays());
                holiday.setCountryCode(resolveCountryCode(doliHoliday.getIdPays()));
                holiday.setActive(doliHoliday.isActive());
                holiday.setUpdatedAt(LocalDateTime.now());

                holidayRepository.save(holiday);
                dolibarrSyncLogService.logSuccess("HOLIDAY", "UPSERT", holiday.getId(), doliHoliday.getId(), SyncDirection.INBOUND, doliHoliday);
                log.debug("✏️ Jour férié mis à jour: {}", doliHoliday.getLibelle());
            } else {
                // Création
                Holiday newHoliday = Holiday.builder()
                        .dolibarrHolidayId(doliHoliday.getId())
                        .libelle(doliHoliday.getLibelle())
                        .dateJour(doliHoliday.getDateJour())
                        .duree(doliHoliday.getDuree() != null ? doliHoliday.getDuree() : 1.0)
                        .idPays(doliHoliday.getIdPays())
                        .countryCode(resolveCountryCode(doliHoliday.getIdPays()))
                        .active(doliHoliday.isActive())
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build();

                holidayRepository.save(newHoliday);
                dolibarrSyncLogService.logSuccess("HOLIDAY", "UPSERT", newHoliday.getId(), doliHoliday.getId(), SyncDirection.INBOUND, doliHoliday);
                log.debug("✨ Nouveau jour férié créé: {}", doliHoliday.getLibelle());
            }

            syncCount++;
        }

        log.info("🎉 {} jours fériés synchronisés", syncCount);
        return syncCount;
    }

    /**
     * Synchronise les allocations de congés par employé
     */
    @Transactional
    public int syncLeaveAllocations() {
        log.info("🎯 Synchronisation des allocations de congés...");

        List<DolibarrLeaveAllocationDto> allocations = dolibarrService.getLeaveAllocationsFromDolibarr();
        int syncCount = 0;

        for (DolibarrLeaveAllocationDto allocation : allocations) {
            if (!allocation.isActive()) {
                log.debug("Skipping inactive allocation for employee: {}", allocation.getEmployeeId());
                continue;
            }

            // Récupérer l'employé
            Optional<UserEntity> employee = userRepository.findByDolibarrId(allocation.getEmployeeId());
            if (employee.isEmpty()) {
                log.warn("Employé avec Dolibarr ID {} non trouvé", allocation.getEmployeeId());
                dolibarrSyncLogService.logFailure(
                        "LEAVE_ALLOCATION", "UPSERT", null, allocation.getId(),
                        SyncDirection.INBOUND, "Employé introuvable", allocation);
                continue;
            }

            // Récupérer le type de congé
            Optional<LeaveType> leaveType = leaveTypeRepository.findByDolibarrLeaveTypeId(allocation.getTypeCongeId());
            if (leaveType.isEmpty()) {
                log.warn("Type de congé {} non trouvé", allocation.getTypeCongeId());
                dolibarrSyncLogService.logFailure(
                        "LEAVE_ALLOCATION", "UPSERT", null, allocation.getId(),
                        SyncDirection.INBOUND, "Type de congé introuvable", allocation);
                continue;
            }

            Optional<EmployeeLeaveAllocation> existing = 
                    allocationRepository.findByDolibarrAllocationId(allocation.getId());

            if (existing.isPresent()) {
                // Mise à jour
                EmployeeLeaveAllocation alloc = existing.get();
                alloc.setJoursInitiaux(allocation.getJoursInitiaux());
                alloc.setJoursUtilises(allocation.getJoursUtilises());
                alloc.setJoursDisponibles(allocation.getJoursDisponibles());
                alloc.setDateDebut(allocation.getDateDebut());
                alloc.setDateFin(allocation.getDateFin());
                alloc.setActive(allocation.isActive());
                alloc.setUpdatedAt(LocalDateTime.now());

                allocationRepository.save(alloc);
                dolibarrSyncLogService.logSuccess("LEAVE_ALLOCATION", "UPSERT", alloc.getId(), allocation.getId(), SyncDirection.INBOUND, allocation);
                log.debug("✏️ Allocation mise à jour pour {} - {}", 
                        employee.get().getPrenom(), leaveType.get().getCode());
            } else {
                // Création
                EmployeeLeaveAllocation newAllocation = EmployeeLeaveAllocation.builder()
                        .employee(employee.get())
                        .leaveType(leaveType.get())
                        .dolibarrAllocationId(allocation.getId())
                        .joursInitiaux(allocation.getJoursInitiaux())
                        .joursUtilises(allocation.getJoursUtilises())
                        .joursDisponibles(allocation.getJoursDisponibles())
                        .annee(allocation.getAnnee())
                        .dateDebut(allocation.getDateDebut())
                        .dateFin(allocation.getDateFin())
                        .active(allocation.isActive())
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build();

                allocationRepository.save(newAllocation);
                dolibarrSyncLogService.logSuccess("LEAVE_ALLOCATION", "UPSERT", newAllocation.getId(), allocation.getId(), SyncDirection.INBOUND, allocation);
                log.debug("✨ Allocation créée pour {} - {}", 
                        employee.get().getPrenom(), leaveType.get().getCode());
            }

            syncCount++;
        }

        log.info("🎯 {} allocations de congés synchronisées", syncCount);
        return syncCount;
    }

    /**
     * Classe interne pour la réponse de synchronisation
     */
    public static class DolibarrFullSyncResponse {
        private boolean success;
        private String message;
        private int employeesSynced;
        private int leaveTypesSynced;
        private int holidaysSynced;
        private int allocationsSynced;

        public boolean isSuccess() {
            return success;
        }

        public void setSuccess(boolean success) {
            this.success = success;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public int getEmployeesSynced() {
            return employeesSynced;
        }

        public void setEmployeesSynced(int employeesSynced) {
            this.employeesSynced = employeesSynced;
        }

        public int getLeaveTypesSynced() {
            return leaveTypesSynced;
        }

        public void setLeaveTypesSynced(int leaveTypesSynced) {
            this.leaveTypesSynced = leaveTypesSynced;
        }

        public int getHolidaysSynced() {
            return holidaysSynced;
        }

        public void setHolidaysSynced(int holidaysSynced) {
            this.holidaysSynced = holidaysSynced;
        }

        public int getAllocationsSynced() {
            return allocationsSynced;
        }

        public void setAllocationsSynced(int allocationsSynced) {
            this.allocationsSynced = allocationsSynced;
        }

        @Override
        public String toString() {
            return "DolibarrFullSyncResponse{" +
                    "success=" + success +
                    ", message='" + message + '\'' +
                    ", employeesSynced=" + employeesSynced +
                    ", leaveTypesSynced=" + leaveTypesSynced +
                    ", holidaysSynced=" + holidaysSynced +
                    ", allocationsSynced=" + allocationsSynced +
                    '}';
        }
    }

    private String resolveCountryCode(Long idPays) {
        if (idPays == null) {
            return "DEFAULT";
        }
        // Mapping minimal et extensible via table si nécessaire.
        if (idPays == 223L) {
            return "TN";
        }
        if (idPays == 153L) {
            return "MA";
        }
        if (idPays == 1L) {
            return "FR";
        }
        return "DEFAULT";
    }
}
