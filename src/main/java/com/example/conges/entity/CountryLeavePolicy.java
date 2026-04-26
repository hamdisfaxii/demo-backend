package com.example.conges.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "country_leave_policies")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CountryLeavePolicy {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 10)
    private String countryCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private TypeConge typeConge;

    @Column(nullable = false)
    private Integer annualQuota;
}
