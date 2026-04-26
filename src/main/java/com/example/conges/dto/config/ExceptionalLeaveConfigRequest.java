package com.example.conges.dto.config;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ExceptionalLeaveConfigRequest {

    @NotBlank
    private String countryCode;

    @NotBlank
    private String label;

    @NotNull
    @Min(0)
    private Integer daysPerYear;

    @NotNull
    private Boolean enabled;
}
