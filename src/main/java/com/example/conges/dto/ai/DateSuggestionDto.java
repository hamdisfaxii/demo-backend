package com.example.conges.dto.ai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * DTO pour une suggestion de date de congé
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DateSuggestionDto {
    
    private LocalDate startDate;
    private LocalDate endDate;
    private int numberOfDays;
    private double score; // Entre 0 et 1 (plus proche de 1 = meilleur)
    private String reason; // Explication de la suggestion
    
    public String getScorePercentage() {
        return String.format("%.0f%%", score * 100);
    }
}
