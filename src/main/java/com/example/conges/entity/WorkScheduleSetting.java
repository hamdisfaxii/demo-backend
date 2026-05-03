package com.example.conges.entity;

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
@Table(name = "work_schedule_settings")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkScheduleSetting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 10)
    private String countryCode;

    @Column(nullable = false, length = 16)
    private String activeType; // NORMAL | SUMMER | RAMADAN

    @Column(nullable = false)
    private Boolean normalEnabled;

    @Column(nullable = false)
    private Boolean summerEnabled;

    @Column(nullable = false)
    private Boolean ramadanEnabled;
}
