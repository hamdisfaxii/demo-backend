package com.example.conges.dto;

import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValiderDemandeRequest {

    @NotNull(message = "Le champ accepte est obligatoire")
    private Boolean accepte;

    private String commentaire;
}
