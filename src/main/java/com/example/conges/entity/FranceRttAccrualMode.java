package com.example.conges.entity;

public enum FranceRttAccrualMode {
    /** Solde complet dès attribution (avec prorata embauche). */
    ANNUAL_JAN1,
    /** Déblocage progressif intra-année (quota annuel × mois écoulés / mois d'emploi cette année). */
    MONTHLY,
    /** Même attribu annuel calculé depuis les heures hebdomadaires (+ prorata embauche) ; comportement équivalent au mode annuel au niveau des plafonds. */
    CONTRACT_HOURS
}
