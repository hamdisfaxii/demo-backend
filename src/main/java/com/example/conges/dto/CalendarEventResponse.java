package com.example.conges.dto;

import java.time.LocalDate;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CalendarEventResponse {
    private String eventType;
    private Long demandeId;
    private Long userId;
    private String employeeName;
    private String department;
    private String country;
    private String leaveType;
    private String title;
    private LocalDate startDate;
    private LocalDate endDate;
}
