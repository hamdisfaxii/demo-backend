package com.example.conges.entity;

import java.time.LocalDateTime;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "france_rtt_settings")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FranceRttSettings {

    @Id
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "accrual_mode", nullable = false, length = 32)
    private FranceRttAccrualMode accrualMode;

    /** Si non null, remplace tout calcul automatique depuis les heures (jours/an). */
    @Column(name = "admin_override_days")
    private Integer adminOverrideDays;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
