package com.example.conges.dto;

import com.example.conges.entity.History.ActionType;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO pour la réponse d'historique
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HistoryResponse {

    private Long id;

    private UserNameDto user;

    private Long demandeId;

    private ActionType actionType;

    private String description;

    private String details;

    private String pays;

    private String statut;

    @JsonFormat(pattern = "dd/MM/yyyy HH:mm:ss")
    private LocalDateTime actionDate;

    private String ipAddress;

    private String userAgent;

    /**
     * DTO pour afficher le nom de d'utilisateur
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserNameDto {
        private Long id;
        private String nom;
        private String prenom;
        private String email;
    }
}
