package com.example.conges.mapper;

import com.example.conges.dto.HistoryResponse;
import com.example.conges.entity.History;
import org.springframework.stereotype.Component;

/**
 * Mapper pour convertir les entités History en DTOs
 */
@Component
public class HistoryMapper {

    /**
     * Convertit une entité History en HistoryResponse
     */
    public HistoryResponse toResponse(History history) {
        if (history == null) {
            return null;
        }

        return HistoryResponse.builder()
                .id(history.getId())
                .user(HistoryResponse.UserNameDto.builder()
                        .id(history.getUser().getId())
                        .nom(history.getUser().getNom())
                        .prenom(history.getUser().getPrenom())
                        .email(history.getUser().getEmail())
                        .build())
                .demandeId(history.getDemande() != null ? history.getDemande().getId() : null)
                .actionType(history.getActionType())
                .description(history.getDescription())
                .details(history.getDetails())
                .pays(history.getPays())
                .statut(history.getStatut())
                .actionDate(history.getActionDate())
                .ipAddress(history.getIpAddress())
                .userAgent(history.getUserAgent())
                .build();
    }
}
