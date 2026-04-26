package com.example.conges.dto.dolibarr;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO pour les types de congés Dolibarr
 * Récupère les types de congés paramétrés dans Dolibarr
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DolibarrLeaveTypeDto {

    @JsonProperty("rowid")
    private Long id;

    @JsonProperty("code")
    private String code;

    @JsonProperty("label")
    private String libelle;

    @JsonProperty("description")
    private String description;

    @JsonProperty("affect_login")
    private Integer affectLogin;

    @JsonProperty("delay")
    private Integer delai;

    @JsonProperty("active")
    private Integer active;

    @JsonProperty("require_approval")
    private Integer requireApproval;

    @JsonProperty("color")
    private String couleur;

    public boolean isActive() {
        return active != null && active == 1;
    }

    public boolean requiresApproval() {
        return requireApproval != null && requireApproval == 1;
    }
}
