package com.example.conges.dto.config;

import java.time.LocalTime;
import lombok.Data;

@Data
public class WorkScheduleDayRequest {
    private Integer dayOfWeek;
    private LocalTime firstStart;
    private LocalTime firstEnd;
    private LocalTime secondStart;
    private LocalTime secondEnd;
}
