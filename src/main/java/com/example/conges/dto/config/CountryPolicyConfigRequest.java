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
}
