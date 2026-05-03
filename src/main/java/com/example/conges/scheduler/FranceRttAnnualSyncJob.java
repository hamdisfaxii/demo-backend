package com.example.conges.scheduler;

import com.example.conges.service.FranceRttLedgerService;
import java.time.LocalDate;
import java.time.Year;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Chaque 1er janvier : resynchronise les lignes cache RTT France (usages lus depuis les demandes approuvées).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class FranceRttAnnualSyncJob {

    private final FranceRttLedgerService franceRttLedgerService;

    @Scheduled(cron = "${france.rtt.annual-cron:0 5 0 1 1 *}")
    public void runFranceRttYearlySync() {
        int y = Year.now().getValue();
        log.info("Tâche RTT France : resynchronisation année {}", y);
        franceRttLedgerService.syncAllEligibleFranceBalancesForYear(y, LocalDate.now());
    }
}
