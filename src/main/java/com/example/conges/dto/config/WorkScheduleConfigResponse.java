package com.example.conges.dto.config;

import java.time.LocalTime;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class WorkScheduleConfigResponse {
    private String countryCode;
    private String scheduleType;
    private String activeType;
    private Boolean normalEnabled;
    private Boolean summerEnabled;
    private Boolean ramadanEnabled;
    private List<Row> rows;

    @Data
    @Builder
    public static class Row {
        private Integer dayOfWeek;
        private LocalTime firstStart;
        private LocalTime firstEnd;
        private LocalTime secondStart;
        private LocalTime secondEnd;
    }
}
