package com.example.conges.repository;

import com.example.conges.entity.History;
import com.example.conges.entity.History.ActionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository JPA pour l'entité History.
 * Fournit des requêtes personnalisées pour filtrer l'historique.
 */
@Repository
public interface HistoryRepository extends JpaRepository<History, Long> {

    /**
     * Récupère l'historique d'un utilisateur spécifique
     */
    Page<History> findByUserId(Long userId, Pageable pageable);

    /**
     * Récupère l'historique d'une demande de congé
     */
    Page<History> findByDemandeId(Long demandeId, Pageable pageable);

    /**
     * Récupère l'historique par type d'action
     */
    Page<History> findByActionType(ActionType actionType, Pageable pageable);

    /**
     * Récupère l'historique par pays
     */
    Page<History> findByPays(String pays, Pageable pageable);

    /**
     * Recherche avancée avec filtres multiples
     */
    @Query("SELECT h FROM History h WHERE " +
            "(:userId IS NULL OR h.user.id = :userId) AND " +
            "(:demandeId IS NULL OR h.demande.id = :demandeId) AND " +
            "(:actionType IS NULL OR h.actionType = :actionType) AND " +
            "(:pays IS NULL OR h.pays = :pays) AND " +
            "(:startDate IS NULL OR h.actionDate >= :startDate) AND " +
            "(:endDate IS NULL OR h.actionDate <= :endDate)")
    Page<History> searchHistory(
            @Param("userId") Long userId,
            @Param("demandeId") Long demandeId,
            @Param("actionType") ActionType actionType,
            @Param("pays") String pays,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable
    );

    /**
     * Recherche pour export (sans pagination), triée du plus récent au plus ancien.
     */
    @Query("SELECT h FROM History h WHERE " +
            "(:userId IS NULL OR h.user.id = :userId) AND " +
            "(:demandeId IS NULL OR h.demande.id = :demandeId) AND " +
            "(:actionType IS NULL OR h.actionType = :actionType) AND " +
            "(:pays IS NULL OR h.pays = :pays) AND " +
            "(:startDate IS NULL OR h.actionDate >= :startDate) AND " +
            "(:endDate IS NULL OR h.actionDate <= :endDate) " +
            "ORDER BY h.actionDate DESC")
    List<History> searchHistoryForExport(
            @Param("userId") Long userId,
            @Param("demandeId") Long demandeId,
            @Param("actionType") ActionType actionType,
            @Param("pays") String pays,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    /**
     * Récupère l'historique entre deux dates (pour exports)
     */
    List<History> findByActionDateBetweenOrderByActionDateDesc(
            LocalDateTime startDate,
            LocalDateTime endDate
    );

    /**
     * Compte le nombre d'actions d'un type spécifique
     */
    Long countByActionType(ActionType actionType);

    /**
     * Récupère les actions d'un utilisateur sur une période
     */
    @Query("SELECT h FROM History h WHERE h.user.id = :userId AND " +
            "h.actionDate BETWEEN :startDate AND :endDate ORDER BY h.actionDate DESC")
    List<History> getUserHistoryByPeriod(
            @Param("userId") Long userId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );
}
