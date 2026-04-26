package com.example.conges.dto.ai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO pour la détection de conflits d'absences
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConflictDetectionDto {
    
    private boolean hasConflict; // True si des conflits détectés
    private String conflictLevel; // LOW, MEDIUM, HIGH
    private List<String> conflicts; // Liste des conflits détectés
    private List<String> recommendations; // Recommandations
    
    public boolean isHighRisk() {
        return "HIGH".equals(conflictLevel);
    }
    
    public boolean isMediumRisk() {
        return "MEDIUM".equals(conflictLevel);
    }
}
