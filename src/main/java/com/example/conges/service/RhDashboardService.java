package com.example.conges.service;

import com.example.conges.dto.EmployeAlerteSoldeDto;
import com.example.conges.dto.RhDashboardResponse;
import com.example.conges.entity.Role;
import com.example.conges.entity.StatutConge;
import com.example.conges.entity.TypeConge;
import com.example.conges.entity.UserEntity;
import com.example.conges.repository.DemandeCongeRepository;
import com.example.conges.repository.JoursPrisParTypeProjection;
import com.example.conges.repository.TypeDemandesCountProjection;
import com.example.conges.repository.UserRepository;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RhDashboardService {

    private static final int QUOTA_CONGES_PAYES_SIMULE = 30;
    private static final int SEUIL_JOURS_ALERTE_PAYES = 5;

    private final DemandeCongeRepository demandeCongeRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public RhDashboardResponse getDashboardComplet() {
        Map<StatutConge, Long> parStatut = getDemandesParStatut();
        long enAttente = parStatut.getOrDefault(StatutConge.EN_ATTENTE, 0L);
        long acceptees = parStatut.getOrDefault(StatutConge.ACCEPTE, 0L);
        long refusees = parStatut.getOrDefault(StatutConge.REFUSE, 0L);
        long annulees = parStatut.getOrDefault(StatutConge.ANNULE, 0L);
        long total = parStatut.values().stream().mapToLong(Long::longValue).sum();
        return RhDashboardResponse.builder()
                .demandesEnAttente(enAttente)
                .demandesAcceptees(acceptees)
                .demandesRefusees(refusees)
                .demandesAnnulees(annulees)
                .demandesTotal(total)
                .demandesParStatut(parStatut)
                .demandesParMois(getDemandesParMois())
                .demandesParType(getDemandesParType())
                .employesAlerteSolde(getEmployesAlerteSolde())
                .tauxValidation(getTauxValidation())
                .build();
    }

    @Transactional(readOnly = true)
    public Map<StatutConge, Long> getDemandesParStatut() {
        Map<StatutConge, Long> map = new EnumMap<>(StatutConge.class);
        for (StatutConge statut : StatutConge.values()) {
            map.put(statut, demandeCongeRepository.countByStatut(statut));
        }
        return map;
    }

    @Transactional(readOnly = true)
    public Map<String, Long> getDemandesParMois() {
        int year = LocalDate.now().getYear();
        Map<String, Long> map = new LinkedHashMap<>();
        for (int m = 1; m <= 12; m++) {
            map.put(String.format("%d-%02d", year, m), 0L);
        }
        List<Object[]> rows = demandeCongeRepository.countDemandesByMonthForYear(year);
        for (Object[] row : rows) {
            int month = ((Number) row[0]).intValue();
            long count = ((Number) row[1]).longValue();
            String key = String.format("%d-%02d", year, month);
            map.put(key, count);
        }
        return map;
    }

    @Transactional(readOnly = true)
    public Map<TypeConge, Long> getDemandesParType() {
        Map<TypeConge, Long> map = new EnumMap<>(TypeConge.class);
        for (TypeConge type : TypeConge.values()) {
            map.put(type, 0L);
        }
        for (TypeDemandesCountProjection row : demandeCongeRepository.countDemandesGroupedByTypeConge()) {
            map.put(row.getTypeConge(), row.getTotal());
        }
        return map;
    }

    @Transactional(readOnly = true)
    public List<EmployeAlerteSoldeDto> getEmployesAlerteSolde() {
        List<EmployeAlerteSoldeDto> alertes = new ArrayList<>();
        EnumSet<StatutConge> statutsSolde = EnumSet.of(StatutConge.ACCEPTE, StatutConge.EN_ATTENTE);

        for (UserEntity employe : userRepository.findByRole(Role.EMPLOYE)) {
            int joursPrisPayes = joursPayesPrisOuReserves(employe.getId(), statutsSolde);
            int restants = QUOTA_CONGES_PAYES_SIMULE - joursPrisPayes;
            if (restants < SEUIL_JOURS_ALERTE_PAYES) {
                alertes.add(EmployeAlerteSoldeDto.builder()
                        .id(employe.getId())
                        .nom(employe.getNom())
                        .prenom(employe.getPrenom())
                        .email(employe.getEmail())
                        .joursRestantsPayes(restants)
                        .build());
            }
        }
        return alertes;
    }

    @Transactional(readOnly = true)
    public Double getTauxValidation() {
        long acceptees = demandeCongeRepository.countByStatut(StatutConge.ACCEPTE);
        long refusees = demandeCongeRepository.countByStatut(StatutConge.REFUSE);
        long totalTraitees = acceptees + refusees;
        if (totalTraitees == 0) {
            return 0.0;
        }
        return Math.round(10000.0 * acceptees / totalTraitees) / 100.0;
    }

    private int joursPayesPrisOuReserves(Long userId, EnumSet<StatutConge> statuts) {
        List<JoursPrisParTypeProjection> rows = demandeCongeRepository.sumJoursPrisParTypePourUtilisateur(
                userId,
                statuts
        );
        return rows.stream()
                .filter(r -> r.getTypeConge() == TypeConge.PAYE)
                .findFirst()
                .map(r -> r.getTotalJours().intValue())
                .orElse(0);
    }
}
