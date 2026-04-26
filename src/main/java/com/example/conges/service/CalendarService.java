package com.example.conges.service;

import com.example.conges.dto.CalendarEventResponse;
import com.example.conges.entity.DemandeConge;
import com.example.conges.entity.Holiday;
import com.example.conges.repository.DemandeCongeRepository;
import com.example.conges.repository.HolidayRepository;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CalendarService {

    private final DemandeCongeRepository demandeCongeRepository;
    private final HolidayRepository holidayRepository;

    @Transactional(readOnly = true)
    public List<CalendarEventResponse> getEvents(
            LocalDate startDate,
            LocalDate endDate,
            Long employeeId,
            String department,
            String country
    ) {
        List<CalendarEventResponse> events = new ArrayList<>();

        List<DemandeConge> leaves = demandeCongeRepository.findApprovedForCalendar(
                startDate,
                endDate,
                employeeId,
                normalizeOptional(department),
                normalizeOptionalCountry(country)
        );
        for (DemandeConge leave : leaves) {
            String fullName = (leave.getUser().getPrenom() + " " + leave.getUser().getNom()).trim();
            events.add(CalendarEventResponse.builder()
                    .eventType("APPROVED_LEAVE")
                    .demandeId(leave.getId())
                    .userId(leave.getUser().getId())
                    .employeeName(fullName)
                    .department(leave.getUser().getDepartement())
                    .country(leave.getUser().getPays())
                    .leaveType(leave.getTypeConge().name())
                    .title(fullName + " - " + leave.getTypeConge().getLibelle())
                    .startDate(leave.getDateDebut())
                    .endDate(leave.getDateFin())
                    .build());
        }

        List<Holiday> holidays = holidayRepository.findByDateRangeWithOptionalCountry(
                startDate,
                endDate,
                normalizeOptionalCountry(country)
        );
        for (Holiday holiday : holidays) {
            events.add(CalendarEventResponse.builder()
                    .eventType("HOLIDAY")
                    .title(holiday.getLibelle())
                    .country(holiday.getCountryCode())
                    .startDate(holiday.getDateJour())
                    .endDate(holiday.getDateJour())
                    .build());
        }
        return events;
    }

    private String normalizeOptionalCountry(String country) {
        if (country == null || country.isBlank()) {
            return null;
        }
        return country.trim().toUpperCase();
    }

    private String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
