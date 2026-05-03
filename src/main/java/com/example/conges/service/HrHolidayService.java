package com.example.conges.service;

import com.example.conges.entity.Holiday;
import com.example.conges.repository.HolidayRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.MonthDay;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
public class HrHolidayService {

    private static final List<String> HR_PUBLIC_HOLIDAY_COUNTRIES = List.of("TN", "FR", "MA");
    private static final long SYNTHETIC_PREFIX = 9_000_000_000L;
    private final HolidayRepository holidayRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    @Transactional(readOnly = true)
    public List<Holiday> listByCountryAndYear(String countryCode, Integer year) {
        String normalized = countryCode == null ? null : countryCode.trim().toUpperCase();
        return holidayRepository.findForHrPanel(normalized, year);
    }

    @Transactional
    public int importPublicHolidays(String countryCode, Integer year) {
        String normalizedCountry = countryCode == null ? "TN" : countryCode.trim().toUpperCase();
        int targetYear = year == null ? LocalDate.now().getYear() : year;
        String url = "https://date.nager.at/api/v3/PublicHolidays/" + targetYear + "/" + normalizedCountry;

        try {
            ResponseEntity<List> response = restTemplate.exchange(url, HttpMethod.GET, null, List.class);
            List<Map<String, Object>> payload = response.getBody();
            if (payload == null || payload.isEmpty()) {
                return importFallbackPublicHolidays(normalizedCountry, targetYear);
            }

            int imported = 0;
            for (Map<String, Object> row : payload) {
                String date = String.valueOf(row.get("date"));
                String localName = row.get("localName") == null ? String.valueOf(row.get("name")) : String.valueOf(row.get("localName"));
                if (date == null || date.isBlank() || localName == null || localName.isBlank()) {
                    continue;
                }
                LocalDate parsedDate = LocalDate.parse(date);
                imported += upsertHoliday(normalizedCountry, parsedDate, localName);
            }

            return imported;
        } catch (Exception ignored) {
            // Fallback robuste pour éviter de bloquer l'action RH quand la source externe échoue.
            return importFallbackPublicHolidays(normalizedCountry, targetYear);
        }
    }

    /**
     * Importe les jours fériés officiels pour TN, FR et MA (une passe par pays).
     */
    @Transactional
    public Map<String, Object> importPublicHolidaysAllCountries(Integer year) {
        int targetYear = year == null ? LocalDate.now().getYear() : year;
        int totalImported = 0;
        List<Map<String, Object>> results = new ArrayList<>();
        for (String cc : HR_PUBLIC_HOLIDAY_COUNTRIES) {
            int imported = importPublicHolidays(cc, targetYear);
            totalImported += imported;
            results.add(Map.of(
                    "country", cc,
                    "imported", imported,
                    "year", targetYear));
        }
        return Map.of(
                "success", true,
                "year", targetYear,
                "countries", HR_PUBLIC_HOLIDAY_COUNTRIES,
                "results", results,
                "totalImported", totalImported);
    }

    @Transactional
    public Holiday createPublicHoliday(String countryCode, String libelle, LocalDate dateJour) {
        String normalizedCountry = countryCode == null ? "TN" : countryCode.trim().toUpperCase();
        String label = libelle == null ? "" : libelle.trim();
        if (label.isBlank()) {
            throw new IllegalArgumentException("Libellé obligatoire");
        }
        if (dateJour == null) {
            throw new IllegalArgumentException("Date obligatoire");
        }

        Holiday existing = holidayRepository
                .findByCountryCodeAndDateJourAndLibelleIgnoreCase(normalizedCountry, dateJour, label)
                .orElse(null);

        if (existing != null) {
            existing.setActive(true);
            existing.setUpdatedAt(LocalDateTime.now());
            return holidayRepository.save(existing);
        }

        Holiday created = Holiday.builder()
                .dolibarrHolidayId(buildSyntheticHolidayId(normalizedCountry, dateJour, label))
                .libelle(label)
                .dateJour(dateJour)
                .duree(1.0)
                .idPays(null)
                .countryCode(normalizedCountry)
                .active(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        return holidayRepository.save(created);
    }

    @Transactional
    public Holiday setAppliedState(Long id, boolean applied) {
        Holiday holiday = holidayRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Jour férié introuvable"));
        holiday.setActive(applied);
        holiday.setUpdatedAt(LocalDateTime.now());
        return holidayRepository.save(holiday);
    }

    @Transactional
    public void deleteHoliday(Long id) {
        holidayRepository.deleteById(id);
    }

    private long buildSyntheticHolidayId(String countryCode, LocalDate date, String label) {
        long hash = Math.abs(Objects.hash(countryCode, date, label));
        return SYNTHETIC_PREFIX + hash;
    }

    private int upsertHoliday(String countryCode, LocalDate date, String label) {
        Holiday existing = holidayRepository
                .findByCountryCodeAndDateJourAndLibelleIgnoreCase(countryCode, date, label)
                .orElse(null);

        if (existing == null) {
            Holiday created = Holiday.builder()
                    .dolibarrHolidayId(buildSyntheticHolidayId(countryCode, date, label))
                    .libelle(label)
                    .dateJour(date)
                    .duree(1.0)
                    .idPays(null)
                    .countryCode(countryCode)
                    .active(true)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            holidayRepository.save(created);
            return 1;
        }

        existing.setActive(true);
        existing.setUpdatedAt(LocalDateTime.now());
        holidayRepository.save(existing);
        return 0;
    }

    private int importFallbackPublicHolidays(String countryCode, int year) {
        List<Map.Entry<MonthDay, String>> entries = switch (countryCode) {
            case "FR" -> List.of(
                    Map.entry(MonthDay.of(1, 1), "Jour de l'An"),
                    Map.entry(MonthDay.of(5, 1), "Fête du Travail"),
                    Map.entry(MonthDay.of(7, 14), "Fête Nationale"),
                    Map.entry(MonthDay.of(11, 11), "Armistice"),
                    Map.entry(MonthDay.of(12, 25), "Noël")
            );
            case "MA" -> List.of(
                    Map.entry(MonthDay.of(1, 11), "Manifeste de l'Indépendance"),
                    Map.entry(MonthDay.of(5, 1), "Fête du Travail"),
                    Map.entry(MonthDay.of(7, 30), "Fête du Trône"),
                    Map.entry(MonthDay.of(8, 14), "Oued Ed-Dahab"),
                    Map.entry(MonthDay.of(11, 18), "Fête de l'Indépendance")
            );
            default -> List.of(
                    Map.entry(MonthDay.of(1, 1), "Nouvel An"),
                    Map.entry(MonthDay.of(3, 20), "Fête de l'Indépendance"),
                    Map.entry(MonthDay.of(4, 9), "Fête des Martyrs"),
                    Map.entry(MonthDay.of(5, 1), "Fête du Travail"),
                    Map.entry(MonthDay.of(7, 25), "Fête de la République")
            );
        };

        int imported = 0;
        for (Map.Entry<MonthDay, String> entry : entries) {
            LocalDate date = entry.getKey().atYear(year);
            imported += upsertHoliday(countryCode, date, entry.getValue());
        }
        return imported;
    }
}
