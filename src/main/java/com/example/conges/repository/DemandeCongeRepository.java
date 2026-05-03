package com.example.conges.repository;

import com.example.conges.entity.DemandeConge;
import com.example.conges.entity.StatutConge;
import com.example.conges.entity.WorkflowStepType;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DemandeCongeRepository extends JpaRepository<DemandeConge, Long> {

    @Query("SELECT DISTINCT d FROM DemandeConge d JOIN FETCH d.user WHERE d.user.id = :userId ORDER BY d.dateSoumission DESC")
    List<DemandeConge> findByUserId(@Param("userId") Long userId);

    @Query("SELECT DISTINCT d FROM DemandeConge d JOIN FETCH d.user WHERE d.user.id = :userId AND d.statut = :statut ORDER BY d.dateSoumission DESC")
    List<DemandeConge> findByUserIdAndStatut(@Param("userId") Long userId, @Param("statut") StatutConge statut);

    @Query("SELECT DISTINCT d FROM DemandeConge d JOIN FETCH d.user WHERE d.statut = :statut ORDER BY d.dateSoumission DESC")
    List<DemandeConge> findByStatut(@Param("statut") StatutConge statut);

    @Query("SELECT DISTINCT d FROM DemandeConge d JOIN FETCH d.user WHERE d.dateDebut BETWEEN :debut AND :fin ORDER BY d.dateDebut")
    List<DemandeConge> findByDateDebutBetween(@Param("debut") LocalDate debut, @Param("fin") LocalDate fin);

    @Query("""
            SELECT d.typeConge AS typeConge, COALESCE(SUM(d.nombreJours), 0) AS totalJours
            FROM DemandeConge d
            WHERE d.user.id = :userId AND d.statut IN :statuts
            GROUP BY d.typeConge
            """)
    List<JoursPrisParTypeProjection> sumJoursPrisParTypePourUtilisateur(
            @Param("userId") Long userId,
            @Param("statuts") Collection<StatutConge> statuts
    );

    long countByStatut(StatutConge statut);
    long countByStatutAndUser_PaysIgnoreCase(StatutConge statut, String pays);

    @Query("SELECT d.typeConge AS typeConge, COUNT(d) AS total FROM DemandeConge d GROUP BY d.typeConge")
    List<TypeDemandesCountProjection> countDemandesGroupedByTypeConge();

    @Query(
            value = "SELECT MONTH(date_soumission) AS mois, COUNT(*) AS cnt FROM demandes_conge "
                    + "WHERE YEAR(date_soumission) = :year GROUP BY MONTH(date_soumission)",
            nativeQuery = true
    )
    List<Object[]> countDemandesByMonthForYear(@Param("year") int year);

    @Query("SELECT DISTINCT d FROM DemandeConge d JOIN FETCH d.user WHERE d.statut = :statut AND d.currentStepType = :stepType ORDER BY d.dateSoumission DESC")
    List<DemandeConge> findByStatutAndCurrentStepType(
            @Param("statut") StatutConge statut,
            @Param("stepType") WorkflowStepType stepType
    );

    @Query("SELECT DISTINCT d FROM DemandeConge d JOIN FETCH d.user WHERE d.statut = :statut AND d.currentStepType IN :stepTypes ORDER BY d.dateSoumission DESC")
    List<DemandeConge> findByStatutAndCurrentStepTypeIn(
            @Param("statut") StatutConge statut,
            @Param("stepTypes") Collection<WorkflowStepType> stepTypes
    );

    // ============= Méthodes pour l'IA Service =============

    @Query("SELECT d FROM DemandeConge d WHERE d.user.id = :userId AND d.statut IN :statuts AND d.dateDebut >= :startDate AND d.dateFin <= :endDate")
    List<DemandeConge> findByUserIdAndStatusAndDateRange(
            @Param("userId") Long userId,
            @Param("statuts") List<StatutConge> statuts,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("SELECT d FROM DemandeConge d WHERE d.dateDebut <= :endDate AND d.dateFin >= :startDate AND d.statut IN ('APPROUVE', 'EN_ATTENTE')")
    List<DemandeConge> findSimultaneousAbsences(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("SELECT COALESCE(SUM(d.nombreJours), 0) FROM DemandeConge d WHERE d.user.id = :userId AND d.statut = 'APPROUVE'")
    long countApprovedDaysForUser(@Param("userId") Long userId);

    @Query("SELECT COALESCE(COUNT(d), 0) FROM DemandeConge d WHERE d.dateDebut <= :endDate AND d.dateFin >= :startDate AND d.statut = 'APPROUVE'")
    int countAbsencesDuring(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query(value = "SELECT COUNT(*) FROM demandes_conge d JOIN users u ON d.user_id = u.id WHERE MONTH(d.date_debut) = :month AND YEAR(d.date_debut) = :year AND u.pays = :pays AND d.statut = 'APPROUVE'", nativeQuery = true)
    long countByMonthAndCountry(
            @Param("year") int year,
            @Param("month") int month,
            @Param("pays") String pays
    );

    @Query("""
            SELECT DISTINCT d
            FROM DemandeConge d
            JOIN FETCH d.user u
            WHERE d.statut = com.example.conges.entity.StatutConge.ACCEPTE
              AND d.dateDebut <= :endDate
              AND d.dateFin >= :startDate
              AND (:employeeId IS NULL OR u.id = :employeeId)
              AND (:department IS NULL OR u.departement = :department)
              AND (:country IS NULL OR u.pays = :country)
            ORDER BY d.dateDebut ASC
            """)
    List<DemandeConge> findApprovedForCalendar(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("employeeId") Long employeeId,
            @Param("department") String department,
            @Param("country") String country
    );

    @Query("""
            SELECT DISTINCT d
            FROM DemandeConge d
            JOIN FETCH d.user u
            WHERE d.statut = com.example.conges.entity.StatutConge.EN_ATTENTE
              AND (:employee IS NULL OR LOWER(CONCAT(COALESCE(u.prenom, ''), ' ', COALESCE(u.nom, ''), ' ', COALESCE(u.email, ''))) LIKE LOWER(CONCAT('%', :employee, '%')))
              AND (:country IS NULL OR UPPER(u.pays) = UPPER(:country))
              AND (:department IS NULL OR LOWER(u.departement) = LOWER(:department))
              AND (:startDate IS NULL OR d.dateDebut >= :startDate)
              AND (:endDate IS NULL OR d.dateFin <= :endDate)
            ORDER BY d.dateSoumission DESC
            """)
    List<DemandeConge> findPendingForHrPanel(
            @Param("employee") String employee,
            @Param("country") String country,
            @Param("department") String department,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );
}
