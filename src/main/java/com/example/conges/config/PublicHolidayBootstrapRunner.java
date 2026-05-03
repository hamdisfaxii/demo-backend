package com.example.conges.config;

import com.example.conges.service.HrHolidayService;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Importe automatiquement les jours fériés officiels (TN, FR, MA) au démarrage pour l’année courante
 * et les années adjacentes, afin que calendriers et écran RH soient à jour sans action manuelle.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PublicHolidayBootstrapRunner implements ApplicationRunner {

    private final HrHolidayService hrHolidayService;

    @Override
    public void run(ApplicationArguments args) {
        int y = LocalDate.now().getYear();
        int[] years = {y - 1, y, y + 1};
        for (int year : years) {
            try {
                hrHolidayService.importPublicHolidaysAllCountries(year);
                log.info("Jours fériés officiels synchronisés pour {}", year);
            } catch (Exception e) {
                log.warn("Synchronisation jours fériés pour {} : {}", year, e.getMessage());
            }
        }
    }
}
