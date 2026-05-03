package com.example.conges.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Ledger RTT France : désactive Dolibarr sur les consommations courte durée France pour éviter le double décompte.
 */
@Data
@Component
@ConfigurationProperties(prefix = "france.rtt")
public class FranceRttProperties {

    /** Moteur local actif pour soldes RTT FR (vérif solde + conso à validation). */
    private boolean localLedgerEnabled = true;

    /** Ne pas consommer l'allocation Dolibarr liée aux RTT quand le ledger local s'applique. */
    private boolean skipDolibarrRttConsume = true;
}
