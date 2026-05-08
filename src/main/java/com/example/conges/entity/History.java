package com.example.conges.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
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
 * <p>Important : cette table est conçue pour être autonome (pas de FK vers {@code users} / {@code demandes_conge})
 * afin de pouvoir fonctionner dans un mode "Dolibarr = source de vérité" et "MariaDB = historique seulement".
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

    /**
     * Identifiant utilisateur (ex: ID Dolibarr, ou ID interne si existant).
     * Pas de FK: on stocke un snapshot minimal et on reste découplé.
     */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "user_nom", length = 150)
    private String userNom;

    @Column(name = "user_prenom", length = 150)
    private String userPrenom;

    @Column(name = "user_email", length = 255)
    private String userEmail;

    /**
     * Identifiant demande (si disponible).
     * Pas de FK: la demande peut être gérée ailleurs (Dolibarr, autre service, etc.).
     */
    @Column(name = "demande_id")
    private Long demandeId;

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
        SUPERADMINS_NOTIFIED, // Email envoyé aux Super Admins
        LOGIN,            // Connexion utilisateur
        LOGOUT,           // Déconnexion utilisateur
        REPORT_VIEWED,    // Rapport consulté
        OTHER             // Autre
    }
}
