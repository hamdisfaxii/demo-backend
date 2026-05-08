package com.example.conges.dto;

import com.example.conges.entity.StatutConge;
import com.example.conges.entity.TypeConge;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DemandeCongeResponse {

    private Long id;
    private TypeConge typeConge;
    private LocalDate dateDebut;
    private LocalDate dateFin;
    private int nombreJours;
    private Double nombreJoursExact;
    private String startHalfDay;
    private String endHalfDay;
    private StatutConge statut;
    private String motif;
    private String commentaireRh;
    private LocalDateTime dateSoumission;
    private LocalDateTime dateTraitement;
    private EmployeInfo employe;
    private ApproverInfo approuvePar;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EmployeInfo {
        private String nom;
        private String prenom;
        private String email;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ApproverInfo {
        private Long id;
        private String nom;
        private String prenom;
        private String email;
    }
}
