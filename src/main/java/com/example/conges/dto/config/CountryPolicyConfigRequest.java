package com.example.conges.dto.config;

import com.example.conges.entity.TypeConge;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CountryPolicyConfigRequest {
    @NotBlank
    private String countryCode;

    @NotNull
    private TypeConge typeConge;

    @NotNull
    @Min(0)
    private Integer annualQuota;

    /** Optionnel : taux mensuel CP (TN 1.83, MA 1.05, FR 2.08 si absent). */
    private Double monthlyAccrualRate;

    /** Pour France / courte durée uniquement. */
    private Boolean rttEnabled;

    /** Quota RTT annuel (France). */
    private Integer rttAnnualDays;
}
