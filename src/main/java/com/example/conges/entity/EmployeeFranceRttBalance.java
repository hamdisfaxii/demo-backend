package com.example.conges.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "employee_france_rtt_balance",
        uniqueConstraints = @UniqueConstraint(name = "uk_user_year", columnNames = {"user_id", "calendar_year"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeFranceRttBalance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Column(name = "calendar_year", nullable = false)
    private int calendarYear;

    @Column(name = "rtt_total", nullable = false, precision = 10, scale = 2)
    private BigDecimal rttTotal;

    @Column(name = "rtt_used", nullable = false, precision = 10, scale = 2)
    private BigDecimal rttUsed;

    @Column(name = "rtt_remaining", nullable = false, precision = 10, scale = 2)
    private BigDecimal rttRemaining;

    @Column(name = "last_rtt_update", nullable = false)
    private LocalDateTime lastRttUpdate;

    @PrePersist
    void prePersist() {
        LocalDateTime n = LocalDateTime.now();
        if (lastRttUpdate == null) {
            lastRttUpdate = n;
        }
        if (rttTotal == null) {
            rttTotal = BigDecimal.ZERO;
        }
        if (rttUsed == null) {
            rttUsed = BigDecimal.ZERO;
        }
        if (rttRemaining == null) {
            rttRemaining = BigDecimal.ZERO;
        }
    }

    @PreUpdate
    void preUpdate() {
        lastRttUpdate = LocalDateTime.now();
    }
}
