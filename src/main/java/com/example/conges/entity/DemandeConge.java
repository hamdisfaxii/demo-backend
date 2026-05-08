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
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "demandes_conge")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DemandeConge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    /** Super Admin désigné pour suivi/validation (champ métier « Approuvé par »). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by_admin_id")
    private UserEntity approvedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_conge", nullable = false, length = 32)
    private TypeConge typeConge;

    @Column(name = "date_debut", nullable = false)
    private LocalDate dateDebut;

    @Column(name = "date_fin", nullable = false)
    private LocalDate dateFin;

    @Column(name = "nombre_jours", nullable = false)
    private int nombreJours;

    /**
     * Durée exacte en jours ouvrés (ex: 0.5 pour demi-journée).
     * Si null : on retombe sur {@link #nombreJours}.
     */
    @Column(name = "nombre_jours_exact", precision = 10, scale = 2)
    private Double nombreJoursExact;

    @Enumerated(EnumType.STRING)
    @Column(name = "start_half_day", length = 16)
    private HalfDay startHalfDay;

    @Enumerated(EnumType.STRING)
    @Column(name = "end_half_day", length = 16)
    private HalfDay endHalfDay;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    @Builder.Default
    private StatutConge statut = StatutConge.EN_ATTENTE;

    @Column(nullable = false, length = 2000)
    private String motif;

    @Column(name = "commentaire_rh", length = 2000)
    private String commentaireRh;

    @Column(name = "date_soumission", nullable = false, updatable = false)
    private LocalDateTime dateSoumission;

    @Column(name = "date_traitement")
    private LocalDateTime dateTraitement;

    @Column(name = "workflow_code", length = 100)
    private String workflowCode;

    @Column(name = "current_step_order")
    private Integer currentStepOrder;

    @Enumerated(EnumType.STRING)
    @Column(name = "current_step_type", length = 50)
    private WorkflowStepType currentStepType;

    @Column(name = "dolibarr_leave_request_id")
    private Long dolibarrLeaveRequestId;

    @Column(name = "permission_heure_debut")
    private LocalTime permissionHeureDebut;

    @Column(name = "permission_heure_fin")
    private LocalTime permissionHeureFin;

    /**
     * Hors France : autorisation courte fixe en minutes (ex. 120) — les compteurs jour restent à 0.
     */
    @Column(name = "duree_permission_minutes")
    private Integer dureePermissionMinutes;

    /**
     * Compte les jours ouvrables (lundi–vendredi) entre deux dates incluses.
     * Si une date est null ou si {@code dateFin} est avant {@code dateDebut}, retourne 0.
     */
    public static int calculerJoursOuvrables(LocalDate debut, LocalDate fin) {
        if (debut == null || fin == null || fin.isBefore(debut)) {
            return 0;
        }
        int count = 0;
        for (LocalDate d = debut; !d.isAfter(fin); d = d.plusDays(1)) {
            DayOfWeek dow = d.getDayOfWeek();
            if (dow != DayOfWeek.SATURDAY && dow != DayOfWeek.SUNDAY) {
                count++;
            }
        }
        return count;
    }

    @PrePersist
    protected void onPersist() {
        if (dateSoumission == null) {
            dateSoumission = LocalDateTime.now();
        }
        if (statut == null) {
            statut = StatutConge.EN_ATTENTE;
        }
        recomputeNombreJoursSafely();
    }

    @PreUpdate
    protected void onUpdate() {
        recomputeNombreJoursSafely();
    }

    /** Permission horaire hors France ({@link #dureePermissionMinutes}) : nombre de jours ouvrés = 0 pour les agrégations CP/RTT. */
    private void recomputeNombreJoursSafely() {
        if (typeConge == TypeConge.COURTE_DUREE
                && dureePermissionMinutes != null
                && dureePermissionMinutes > 0) {
            nombreJours = 0;
            nombreJoursExact = 0d;
            return;
        }
        double exact = calculerJoursOuvrablesExact(dateDebut, dateFin, startHalfDay, endHalfDay);
        nombreJoursExact = exact;
        // Compat : garder un entier non négatif (historique/UI legacy).
        nombreJours = Math.max(0, (int) Math.round(exact));
    }

    /**
     * Durée exacte en jours ouvrés (lun–ven) avec demi-journées possibles.
     * Règles :
     * - Si demi-journée (start/end) fournie, elle ne s'applique que si la date correspondante est ouvrée.
     * - Interdit : même date avec start=AFTERNOON et end=MORNING.
     */
    public static double calculerJoursOuvrablesExact(
            LocalDate debut,
            LocalDate fin,
            HalfDay startHalf,
            HalfDay endHalf
    ) {
        if (debut == null || fin == null || fin.isBefore(debut)) {
            return 0d;
        }
        int days = calculerJoursOuvrables(debut, fin);
        if (days <= 0) return 0d;

        boolean startIsWorkday = isWorkday(debut);
        boolean endIsWorkday = isWorkday(fin);

        // Cas même jour
        if (Objects.equals(debut, fin)) {
            if (!startIsWorkday) return 0d;
            if (startHalf == null && endHalf == null) return 1d;
            HalfDay s = startHalf == null ? HalfDay.MORNING : startHalf;
            HalfDay e = endHalf == null ? HalfDay.AFTERNOON : endHalf;
            if (s == HalfDay.AFTERNOON && e == HalfDay.MORNING) return 0d; // invalide => traité comme 0
            if (s == HalfDay.MORNING && e == HalfDay.MORNING) return 0.5d;
            if (s == HalfDay.AFTERNOON && e == HalfDay.AFTERNOON) return 0.5d;
            return 1d;
        }

        double exact = days;
        if (startHalf != null && startIsWorkday) {
            // demi-journée au début => -0.5
            exact -= 0.5d;
        }
        if (endHalf != null && endIsWorkday) {
            // demi-journée à la fin => -0.5
            exact -= 0.5d;
        }
        return Math.max(0d, exact);
    }

    private static boolean isWorkday(LocalDate d) {
        DayOfWeek dow = d.getDayOfWeek();
        return dow != DayOfWeek.SATURDAY && dow != DayOfWeek.SUNDAY;
    }

    /** Durée exacte (fallback sur entier si exact absent). */
    public double getNombreJoursExactOrInt() {
        if (nombreJoursExact == null || nombreJoursExact.isNaN() || nombreJoursExact.isInfinite()) {
            return Math.max(0, nombreJours);
        }
        return Math.max(0d, nombreJoursExact);
    }

    public static int minutesBetween(LocalTime start, LocalTime end) {
        if (start == null || end == null || !end.isAfter(start)) {
            return 0;
        }
        return (int) ChronoUnit.MINUTES.between(start, end);
    }
}
