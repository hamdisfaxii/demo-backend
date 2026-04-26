package com.example.conges.dto.dolibarr;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO pour les allocations de congés par employé
 * Récupère les jours alloués pour chaque employé et type de congé
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DolibarrLeaveAllocationDto {

    @JsonProperty("rowid")
    private Long id;

    @JsonProperty("fk_user")
    private Long employeeId;

    @JsonProperty("fk_leave_type")
    private Long typeCongeId;

    @JsonProperty("qty_init")
    private Double joursInitiaux;

    @JsonProperty("qty_used")
    private Double joursUtilises;

    @JsonProperty("qty_available")
    private Double joursDisponibles;

    @JsonProperty("year_select")
    private Integer annee;

    @JsonProperty("date_start")
    private LocalDate dateDebut;

    @JsonProperty("date_end")
    private LocalDate dateFin;

    @JsonProperty("active")
    private Integer active;

    public boolean isActive() {
        return active != null && active == 1;
    }

    public Double getJoursDisponibles() {
        if (joursDisponibles != null) {
            return joursDisponibles;
        }
        // Calcul si non fourni : initial - utilisés
        if (joursInitiaux != null && joursUtilises != null) {
            return joursInitiaux - joursUtilises;
        }
        return 0.0;
    }
}
