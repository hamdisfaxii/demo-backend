package com.example.conges.dto.dolibarr;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO pour représenter un employé Dolibarr
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DolibarrEmployeeDto {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("firstname")
    private String firstName;

    @JsonProperty("lastname")
    private String lastName;

    @JsonProperty("email")
    private String email;

    @JsonProperty("phone")
    private String phone;

    @JsonProperty("address")
    private String address;

    @JsonProperty("country_id")
    private Integer countryId;

    @JsonProperty("country_code")
    private String countryCode;

    @JsonProperty("login")
    private String login;

    @JsonProperty("job")
    private String job;

    @JsonProperty("note_public")
    private String note;

    @JsonProperty("status")
    private Integer status;  // 0 = Inactive, 1 = Active

    @JsonProperty("usertype")
    private Integer userType;  // Type d'utilisateur (1 = Internal, 2 = External)

    /**
     * Vérifie si l'employé est actif et peut accéder au système
     */
    public boolean isActive() {
        return status != null && status == 1;
    }

    /**
     * Récupère le nom complet de l'employé
     */
    public String getFullName() {
        return (this.firstName != null ? this.firstName : "") + " " + 
               (this.lastName != null ? this.lastName : "");
    }
}
