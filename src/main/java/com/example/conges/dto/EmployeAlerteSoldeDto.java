package com.example.conges.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeAlerteSoldeDto {

    private Long id;
    private String nom;
    private String prenom;
    private String email;
    /** Jours de congés payés restants (quota simulé − pris/réservé). */
    private int joursRestantsPayes;
}
