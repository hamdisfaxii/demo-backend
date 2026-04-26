package com.example.conges.dto.dolibarr;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO pour les jours fériés Dolibarr
 * Récupère les jours fériés de Dolibarr (holidays/bank holidays)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DolibarrHolidayDto {

    @JsonProperty("rowid")
    private Long id;

    @JsonProperty("label")
    private String libelle;

    @JsonProperty("dateh")
    private LocalDate dateJour;

    @JsonProperty("duration")
    private Double duree;

    @JsonProperty("entity")
    private Integer entity;

    @JsonProperty("fk_country")
    private Long idPays;

    @JsonProperty("active")
    private Integer active;

    public boolean isActive() {
        return active != null && active == 1;
    }
}
