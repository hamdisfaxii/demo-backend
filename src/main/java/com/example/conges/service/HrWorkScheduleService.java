package com.example.conges.service;

import com.example.conges.dto.DemandeCongeRequest;
import com.example.conges.dto.config.WorkScheduleConfigRequest;
import com.example.conges.dto.config.WorkScheduleConfigResponse;
import com.example.conges.dto.config.WorkScheduleDayRequest;
import com.example.conges.entity.TypeConge;
import com.example.conges.entity.UserEntity;
import com.example.conges.entity.WorkScheduleDay;
import com.example.conges.entity.WorkScheduleSetting;
import com.example.conges.repository.WorkScheduleDayRepository;
import com.example.conges.repository.WorkScheduleSettingRepository;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class HrWorkScheduleService {

    private final CountryPolicyService countryPolicyService;

    public static final String NORMAL = "NORMAL";
    public static final String SUMMER = "SUMMER";
    public static final String RAMADAN = "RAMADAN";

    private final WorkScheduleDayRepository workScheduleDayRepository;
    private final WorkScheduleSettingRepository workScheduleSettingRepository;

    @Transactional(readOnly = true)
    public WorkScheduleConfigResponse getConfig(String countryCode, String scheduleType) {
        String country = normalizeCountry(countryCode);
        WorkScheduleSetting setting = getOrCreateSetting(country);
        String resolvedType = scheduleType == null ? setting.getActiveType() : normalizeType(scheduleType);
        List<WorkScheduleDay> rows = getOrCreateRows(country, resolvedType);
        return toResponse(setting, resolvedType, rows);
    }

    @Transactional
    public WorkScheduleConfigResponse saveConfig(WorkScheduleConfigRequest request) {
        String country = normalizeCountry(request.getCountryCode());
        String scheduleType = normalizeType(request.getScheduleType());
        WorkScheduleSetting setting = getOrCreateSetting(country);
        String nextActiveType = request.getActiveType() == null
                ? setting.getActiveType()
                : normalizeType(request.getActiveType());

        setting.setActiveType(nextActiveType);
        setting.setNormalEnabled(NORMAL.equals(nextActiveType));
        setting.setSummerEnabled(SUMMER.equals(nextActiveType));
        setting.setRamadanEnabled(RAMADAN.equals(nextActiveType));
        workScheduleSettingRepository.save(setting);

        List<WorkScheduleDay> existing = getOrCreateRows(country, scheduleType);
        Map<Integer, WorkScheduleDay> byDay = existing.stream()
                .collect(Collectors.toMap(WorkScheduleDay::getDayOfWeek, it -> it));

        List<WorkScheduleDayRequest> incoming = request.getRows() == null ? List.of() : request.getRows();
        for (WorkScheduleDayRequest row : incoming) {
            if (row.getDayOfWeek() == null) {
                continue;
            }
            WorkScheduleDay target = byDay.get(row.getDayOfWeek());
            if (target == null) {
                target = WorkScheduleDay.builder()
                        .countryCode(country)
                        .scheduleType(scheduleType)
                        .dayOfWeek(row.getDayOfWeek())
                        .build();
            }
            target.setFirstStart(row.getFirstStart());
            target.setFirstEnd(row.getFirstEnd());
            target.setSecondStart(row.getSecondStart());
            target.setSecondEnd(row.getSecondEnd());
            workScheduleDayRepository.save(target);
        }

        List<WorkScheduleDay> rows = getOrCreateRows(country, scheduleType);
        return toResponse(setting, scheduleType, rows);
    }

    @Transactional(readOnly = true)
    public void validatePermissionWithinWorkingHours(UserEntity user, DemandeCongeRequest request) {
        if (request.getTypeConge() != TypeConge.COURTE_DUREE) {
            return;
        }
        LocalTime start = request.getHeureDebut();
        LocalTime end = request.getHeureFin();
        if (start == null || end == null) {
            throw new IllegalArgumentException("Pour une permission courte durée, les heures début/fin sont obligatoires.");
        }
        if (!end.isAfter(start)) {
            throw new IllegalArgumentException("L'heure de fin doit être supérieure à l'heure de début.");
        }
        LocalDate date = request.getDateDebut();
        if (date == null) {
            throw new IllegalArgumentException("La date de la permission est obligatoire.");
        }
        int dayKey = date.getDayOfWeek().getValue() % 7; // dimanche -> 0

        String country = normalizeCountry(user.getPays());
        WorkScheduleSetting setting = getOrCreateSetting(country);
        List<WorkScheduleDay> rows = getOrCreateRows(country, setting.getActiveType());
        WorkScheduleDay row = rows.stream()
                .filter(r -> r.getDayOfWeek() == dayKey)
                .findFirst()
                .orElse(null);
        if (row == null
                || (!isWithin(row.getFirstStart(), row.getFirstEnd(), start, end)
                        && !isWithin(row.getSecondStart(), row.getSecondEnd(), start, end))) {
            throw new IllegalArgumentException("Permission hors horaires autorisés pour ce jour.");
        }

        String paysMetier = countryPolicyService.normalizeBusinessCountry(user.getPays());
        if (!"FR".equals(paysMetier)) {
            long mins = ChronoUnit.MINUTES.between(start, end);
            if (mins != CountryPolicyService.NON_FR_SHORT_LEAVE_MINUTES) {
                throw new IllegalArgumentException(
                        "Hors France, la durée autorisée pour une autorisation courte est exactement 2 heures.");
            }
        }
    }

    private boolean isWithin(LocalTime sessionStart, LocalTime sessionEnd, LocalTime start, LocalTime end) {
        if (sessionStart == null || sessionEnd == null) {
            return false;
        }
        return !start.isBefore(sessionStart) && !end.isAfter(sessionEnd);
    }

    private WorkScheduleConfigResponse toResponse(WorkScheduleSetting setting, String scheduleType, List<WorkScheduleDay> rows) {
        return WorkScheduleConfigResponse.builder()
                .countryCode(setting.getCountryCode())
                .scheduleType(scheduleType)
                .activeType(setting.getActiveType())
                .normalEnabled(setting.getNormalEnabled())
                .summerEnabled(setting.getSummerEnabled())
                .ramadanEnabled(setting.getRamadanEnabled())
                .rows(rows.stream().map(r -> WorkScheduleConfigResponse.Row.builder()
                        .dayOfWeek(r.getDayOfWeek())
                        .firstStart(r.getFirstStart())
                        .firstEnd(r.getFirstEnd())
                        .secondStart(r.getSecondStart())
                        .secondEnd(r.getSecondEnd())
                        .build()).toList())
                .build();
    }

    private List<WorkScheduleDay> getOrCreateRows(String country, String type) {
        List<WorkScheduleDay> rows = workScheduleDayRepository
                .findByCountryCodeAndScheduleTypeOrderByDayOfWeekAsc(country, type);
        if (!rows.isEmpty()) {
            return rows;
        }
        List<WorkScheduleDay> defaults = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            WorkScheduleDay day = WorkScheduleDay.builder()
                    .countryCode(country)
                    .scheduleType(type)
                    .dayOfWeek(i)
                    .build();
            if (i >= 1 && i <= 5) {
                if (SUMMER.equals(type)) {
                    day.setFirstStart(LocalTime.of(8, 0));
                    day.setFirstEnd(LocalTime.of(14, 0));
                } else if (RAMADAN.equals(type)) {
                    day.setFirstStart(LocalTime.of(9, 0));
                    day.setFirstEnd(LocalTime.of(15, 0));
                } else {
                    day.setFirstStart(LocalTime.of(8, 0));
                    day.setFirstEnd(LocalTime.of(12, 0));
                    day.setSecondStart(LocalTime.of(13, 0));
                    day.setSecondEnd(LocalTime.of(17, 0));
                }
            }
            defaults.add(workScheduleDayRepository.save(day));
        }
        return defaults;
    }

    private WorkScheduleSetting getOrCreateSetting(String country) {
        Optional<WorkScheduleSetting> existing = workScheduleSettingRepository.findByCountryCode(country);
        if (existing.isPresent()) {
            return existing.get();
        }
        return workScheduleSettingRepository.save(WorkScheduleSetting.builder()
                .countryCode(country)
                .activeType(NORMAL)
                .normalEnabled(true)
                .summerEnabled(true)
                .ramadanEnabled(true)
                .build());
    }

    private String normalizeCountry(String raw) {
        String c = String.valueOf(raw == null ? "TN" : raw).trim().toUpperCase(Locale.ROOT);
        if (c.isBlank()) {
            return "TN";
        }
        return c;
    }

    private String normalizeType(String raw) {
        String t = String.valueOf(raw == null ? NORMAL : raw).trim().toUpperCase(Locale.ROOT);
        if (!List.of(NORMAL, SUMMER, RAMADAN).contains(t)) {
            return NORMAL;
        }
        return t;
    }
}
