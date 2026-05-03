package com.example.conges.dto.config;

import com.example.conges.entity.FranceRttAccrualMode;
import java.time.LocalDateTime;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import lombok.Data;

@Data
public class FranceRttSettingsDto {

    @NotNull
    private FranceRttAccrualMode accrualMode;

    /** Optionnel ; null = automatique depuis heures ou politique RH. */
    @Min(0)
    private Integer adminOverrideDays;

    private LocalDateTime updatedAt;
}
