package com.example.conges.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Déclenche une synchro Dolibarr hors du thread HTTP (après connexion réussie).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DolibarrBackgroundSyncFacade {

    private final DolibarrSyncService dolibarrSyncService;

    @Async("taskExecutor")
    public void triggerFullSyncAfterLogin() {
        try {
            DolibarrSyncService.DolibarrFullSyncResponse r =
                    dolibarrSyncService.syncAllDataFromDolibarr();
            log.info(
                    "Synchro Dolibarr post-login terminée — success={}, employés={}, types={}, feriés={}, allocations={}",
                    r.isSuccess(),
                    r.getEmployeesSynced(),
                    r.getLeaveTypesSynced(),
                    r.getHolidaysSynced(),
                    r.getAllocationsSynced());
        } catch (Exception e) {
            log.error("Synchro Dolibarr post-login échouée: {}", e.getMessage(), e);
        }
    }
}
