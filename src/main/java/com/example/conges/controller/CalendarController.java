package com.example.conges.controller;

import com.example.conges.dto.CalendarEventResponse;
import com.example.conges.service.CalendarService;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/calendar")
@RequiredArgsConstructor
public class CalendarController {

    private final CalendarService calendarService;

    @GetMapping("/events")
    @PreAuthorize("hasAnyRole('RH','MANAGER','ADMIN')")
    public ResponseEntity<List<CalendarEventResponse>> getEvents(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long employeeId,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) String country
    ) {
        return ResponseEntity.ok(calendarService.getEvents(
                startDate,
                endDate,
                employeeId,
                department,
                country
        ));
    }
}
