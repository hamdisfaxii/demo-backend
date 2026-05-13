package com.example.conges.dto.dolibarr;

import com.fasterxml.jackson.annotation.JsonAlias;
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
    @JsonAlias({ "fk_user_abs", "userid", "user_id", "fk_employee" })
    private Long employeeId;

    @JsonProperty("fk_leave_type")
    @JsonAlias({ "fk_type", "fk_holiday_type", "type_id" })
    private Long typeCongeId;

    @JsonProperty("leave_type_code")
    @JsonAlias({ "type_code", "code" })
    private String typeCongeCode;

    @JsonProperty("qty_init")
    @JsonAlias({ "qty_total", "nb_init" })
    private Double joursInitiaux;

    @JsonProperty("qty_used")
    @JsonAlias({ "qty_consumed", "nb_used" })
    private Double joursUtilises;

    @JsonProperty("qty_available")
    @JsonAlias({ "nb_holiday", "qty_remain", "balance", "solde" })
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
        return active == null || active == 1;
    }

    /** Solde disponible même si Dolibarr n’expose que qty_init/qty_used (ou nb_holiday seul via alias). */
    public double resolveEffectiveQtyAvailable() {
        if (joursDisponibles != null && !Double.isNaN(joursDisponibles)) {
            return joursDisponibles;
        }
        if (joursInitiaux != null && joursUtilises != null && !Double.isNaN(joursInitiaux) && !Double.isNaN(joursUtilises)) {
            return joursInitiaux - joursUtilises;
        }
        if (joursInitiaux != null && !Double.isNaN(joursInitiaux)) {
            return joursInitiaux;
        }
        return 0.0;
    }
}
