package com.example.conges.dto.config;

import java.util.List;
import lombok.Data;

@Data
public class WorkScheduleConfigRequest {
    private String countryCode;
    private String scheduleType;
    private String activeType;
    private Boolean normalEnabled;
    private Boolean summerEnabled;
    private Boolean ramadanEnabled;
    private List<WorkScheduleDayRequest> rows;
}
