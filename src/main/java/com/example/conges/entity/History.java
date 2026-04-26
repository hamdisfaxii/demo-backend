package com.example.conges.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.PrePersist;
import javax.persistence.Table;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entité pour tracer l'historique de toutes les actions sur les demandes de congés.
 * Logs automatiques et filtrage/export possibles.
 */
@Entity
@Table(name = "history", indexes = {
        @javax.persistence.Index(name = "idx_user", columnList = "user_id"),
        @javax.persistence.Index(name = "idx_demande", columnList = "demande_id"),
        @javax.persistence.Index(name = "idx_action_type", columnList = "action_type"),
        @javax.persistence.Index(name = "idx_action_date", columnList = "action_date")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class History {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "demande_id")
    private DemandeConge demande;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false, length = 50)
    private ActionType actionType;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "details", columnDefinition = "LONGTEXT")
    private String details; // JSON avec infos détaillées (ancien statut, nouveau statut, etc.)

    @Column(name = "pays", length = 50)
    private String pays;

    @Column(name = "statut", length = 50)
    private String statut;

    @Column(name = "action_date", nullable = false, updatable = false)
    private LocalDateTime actionDate;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @PrePersist
    protected void onPersist() {
        if (actionDate == null) {
            actionDate = LocalDateTime.now();
        }
    }

    /**
     * Types d'actions tracées
     */
    public enum ActionType {
        CREATE,           // Création d'une demande
        SUBMIT,           // Soumission d'une demande
        APPROVE,          // Approbation par manager/RH
        REJECT,           // Rejet d'une demande
        CANCEL,           // Annulation d'une demande
        UPDATE,           // Modification d'une demande
        DOCUMENT_SENT,    // Document envoyé
        EXPORTED,         // Export effectué
        SYNCED_DOLIBARR,  // Synchronisation avec Dolibarr
        LOGIN,            // Connexion utilisateur
        LOGOUT,           // Déconnexion utilisateur
        REPORT_VIEWED,    // Rapport consulté
        OTHER             // Autre
    }
}
