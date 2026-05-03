package com.example.conges.dto;

import com.example.conges.entity.TypeConge;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SoldeCongeResponse {

    private TypeConge typeConge;
    private int totalJours;
    private int joursPris;
    private int joursRestants;

    /** Précision décimale (ex. RTT mensuel cumul FR) ; null si hors périmètre. */
    private Double totalExact;
    private Double prisExact;
    private Double restantsExact;
}
