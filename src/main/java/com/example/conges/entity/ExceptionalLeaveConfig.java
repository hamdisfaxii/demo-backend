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
@Table(name = "exceptional_leave_configs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExceptionalLeaveConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 10)
    private String countryCode;

    @Column(nullable = false, length = 120)
    private String label;

    @Column(nullable = false)
    private Integer daysPerYear;

    @Column(nullable = false)
    private Boolean enabled;
}
