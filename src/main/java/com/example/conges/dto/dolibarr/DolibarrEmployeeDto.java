package com.example.conges.dto.dolibarr;

import com.fasterxml.jackson.annotation.JsonAlias;
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
    @JsonAlias({"rowid"})
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
    @JsonAlias({"fk_country"})
    private Integer countryId;

    @JsonProperty("country_code_iso")
    @JsonAlias({"country_code", "code_iso"})
    private String countryCodeIso;

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

    @JsonProperty("admin")
    private Integer admin;

    @JsonProperty("superadmin")
    private Integer superAdmin;

    /**
     * Champ historique utilisé dans le projet : code pays (alpha2/3 selon Dolibarr).
     */
    public String getCountryCode() {
        return countryCodeIso;
    }

    public void setCountryCode(String code) {
        this.countryCodeIso = code;
    }

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

    public boolean isAdminLike() {
        return (admin != null && admin == 1) || (superAdmin != null && superAdmin == 1);
    }
}
