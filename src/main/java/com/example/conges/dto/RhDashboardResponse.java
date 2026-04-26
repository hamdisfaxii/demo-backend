package com.example.conges.dto;

import com.example.conges.entity.StatutConge;
import com.example.conges.entity.TypeConge;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RhDashboardResponse {

    private Map<StatutConge, Long> demandesParStatut;
    private Map<String, Long> demandesParMois;
    private Map<TypeConge, Long> demandesParType;
    private List<EmployeAlerteSoldeDto> employesAlerteSolde;
    /** Pourcentage de demandes acceptées parmi les demandes traitées (acceptée + refusée). */
    private Double tauxValidation;
}
