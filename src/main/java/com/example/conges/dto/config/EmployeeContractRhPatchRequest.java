package com.example.conges.dto.config;

import java.math.BigDecimal;
import lombok.Data;

/** Mise à jour contrat depuis le module RH (ex. cadre légal FR RTT). */
@Data
public class EmployeeContractRhPatchRequest {

    private BigDecimal weeklyHours;
    private Integer annualWorkDays;
    private String contractType;
    private Boolean contractActive;
    private String countryCode;
}
