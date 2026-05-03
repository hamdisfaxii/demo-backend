package com.example.conges.entity;

import java.time.LocalTime;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "work_schedule_days")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkScheduleDay {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 10)
    private String countryCode;

    @Column(nullable = false, length = 16)
    private String scheduleType; // NORMAL | SUMMER | RAMADAN

    @Column(nullable = false)
    private Integer dayOfWeek; // 0: dimanche ... 6: samedi

    private LocalTime firstStart;
    private LocalTime firstEnd;
    private LocalTime secondStart;
    private LocalTime secondEnd;
}
