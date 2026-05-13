package com.example.conges.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TypeConge {
    PAYE("Congé payé"),
    MALADIE("Congé maladie"),
    SANS_SOLDE("Congé sans solde"),
    COURTE_DUREE("Congé de courte durée"),
    EXCEPTIONNEL("Congé exceptionnel");

    private final String libelle;
}
