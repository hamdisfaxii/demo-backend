package com.example.conges.entity;

import com.example.conges.dto.dolibarr.DolibarrHolidayDto;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entité pour stocker les jours fériés synchronisés depuis Dolibarr
 */
@Entity
@Table(name = "holidays")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Holiday {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private Long dolibarrHolidayId;

    @Column(nullable = false)
    private String libelle;

    @Column(nullable = false)
    private LocalDate dateJour;

    @Column(nullable = false)
    private Double duree; // durée en jours (généralement 1.0)

    private Long idPays;

    @Column(length = 10)
    private String countryCode;

    @Column(nullable = false)
    private Boolean active;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public static Holiday fromDto(DolibarrHolidayDto dto) {
        return Holiday.builder()
                .dolibarrHolidayId(dto.getId())
                .libelle(dto.getLibelle())
                .dateJour(dto.getDateJour())
                .duree(dto.getDuree() != null ? dto.getDuree() : 1.0)
                .idPays(dto.getIdPays())
                .countryCode(null)
                .active(dto.isActive())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }
}
