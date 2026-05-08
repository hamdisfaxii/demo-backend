/**
 * Services métier de la plateforme congés.
 *
 * <p><b>Calculs et règles par pays</b> : appliqués dans cette application
 * ({@link com.example.conges.service.CountryPolicyService}, {@link com.example.conges.service.CongeService},
 * {@link com.example.conges.service.FranceRttLedgerService}, etc.). Dolibarr ne porte pas cette logique métier.
 *
 * <p><b>Rôle de Dolibarr</b> : stocker et synchroniser les données RH exposées par l’ERP (soldes dans
 * {@code holiday_users} / API allocations, types, utilisateurs). L’application lit ces montants après synchro,
 * applique ses règles, puis <em>réécrit</em> vers Dolibarr les valeurs validées (approbation, correction RH) pour
 * garder le référentiel aligné.
 */
package com.example.conges.service;
