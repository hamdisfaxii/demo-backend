package com.example.conges.dto.config;

import java.time.LocalDate;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PublicHolidayCreateRequest {

    @NotBlank
    private String countryCode;

    @NotBlank
    private String libelle;

    @NotNull
    private LocalDate dateJour;
}
