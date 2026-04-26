package com.example.conges.dto;

import com.example.conges.entity.TypeConge;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DemandeCongeRequest {

    // Accepte soit "Congé payé", "Congé sans solde", "Congé maladie" depuis le frontend
    @NotBlank(message = "Le titre du congé est obligatoire")
    private String titre;

    @NotNull(message = "La date de début est obligatoire")
    private LocalDate dateDebut;

    @NotNull(message = "La date de fin est obligatoire")
    private LocalDate dateFin;

    // Accepte commentaire ou motif indifféremment
    private String commentaire;

    // Getter pour obtenir le TypeConge basé sur le titre
    public TypeConge getTypeConge() {
        if (titre == null) {
            return TypeConge.PAYE;
        }
        String normalized = titre.toLowerCase().trim();
        if (normalized.contains("maladie")) {
            return TypeConge.MALADIE;
        }
        if (normalized.contains("sans solde")) {
            return TypeConge.SANS_SOLDE;
        }
        return TypeConge.PAYE;
    }

    // Getter pour le motif (alias de commentaire)
    public String getMotif() {
        return commentaire != null ? commentaire : "";
    }
}
