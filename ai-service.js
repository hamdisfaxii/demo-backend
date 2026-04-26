/**
 * Service d'IA pour Suggestions Intelligentes
 * Recommandations de dates et détection de conflits
 */

const moment = require("moment");

class AIService {
  /**
   * Suggérer les meilleures dates pour un congé
   */
  suggestOptimalDates(
    employee,
    leaveType,
    preferredDuration,
    existingDemandes,
  ) {
    const suggestions = [];
    const now = moment();
    const nextYear = moment().add(1, "year");

    // Dates occupées par cet employé
    const busyDates = existingDemandes
      .filter((d) => d.employee_id === employee.id && d.statut !== "REJECTED")
      .flatMap((d) => {
        const dates = [];
        const start = moment(d.date_debut);
        const end = moment(d.date_fin);
        for (
          let day = moment(start);
          day.isSameOrBefore(end);
          day.add(1, "day")
        ) {
          dates.push(day.format("YYYY-MM-DD"));
        }
        return dates;
      });

    // Trouver des périodes libres
    for (
      let startDate = moment(now).add(7, "days");
      startDate.isSameOrBefore(nextYear);
      startDate.add(1, "day")
    ) {
      if (busyDates.includes(startDate.format("YYYY-MM-DD"))) {
        continue; // Occupé
      }

      const endDate = moment(startDate).add(preferredDuration - 1, "days");

      // Vérifier que la période est libre
      let isFree = true;
      for (
        let day = moment(startDate);
        day.isSameOrBefore(endDate);
        day.add(1, "day")
      ) {
        if (busyDates.includes(day.format("YYYY-MM-DD"))) {
          isFree = false;
          break;
        }
      }

      if (isFree) {
        // Calculer le score (plus bientôt + week-end favorable = mieux)
        const daysUntilStart = startDate.diff(now, "days");
        const hasWeekend = this._hasWeekendDays(startDate, endDate);
        const score = 100 - daysUntilStart + (hasWeekend ? 10 : 0);

        suggestions.push({
          startDate: startDate.format("YYYY-MM-DD"),
          endDate: endDate.format("YYYY-MM-DD"),
          duration: preferredDuration,
          daysUntilStart,
          hasWeekend,
          score,
          recommendation: this._generateRecommendationText(
            startDate,
            endDate,
            hasWeekend,
          ),
        });

        if (suggestions.length >= 5) break; // Max 5 suggestions
      }
    }

    return suggestions.sort((a, b) => b.score - a.score);
  }

  /**
   * Détecter les conflits (absences critiques)
   */
  detectConflicts(demande, allDemandes, employees) {
    const conflicts = {
      severity: "LOW", // LOW, MEDIUM, HIGH, CRITICAL
      details: [],
      affectedEmployees: 0,
      recommendation: null,
    };

    const demandeStart = moment(demande.date_debut);
    const demandeEnd = moment(demande.date_fin);

    // Compter les absences sur la même période dans l'équipe
    const overlappingRequests = allDemandes.filter(
      (d) =>
        d.employee_id !== demande.employee_id &&
        d.statut === "APPROVED" &&
        moment(d.date_debut).isSameOrBefore(demandeEnd) &&
        moment(d.date_fin).isSameOrAfter(demandeStart),
    );

    conflicts.affectedEmployees = overlappingRequests.length;

    if (overlappingRequests.length > 0) {
      conflicts.details = overlappingRequests.map((d) => ({
        employeeName: d.employee_name,
        leaveType: d.type_conge,
        dates: `${d.date_debut} à ${d.date_fin}`,
        days: d.nombre_jours,
      }));
    }

    // Évaluer la sévérité
    if (overlappingRequests.length === 0) {
      conflicts.severity = "LOW";
      conflicts.recommendation =
        "Aucun conflit détecté. Demande peut être approuvée sans risque.";
    } else if (overlappingRequests.length <= 2) {
      conflicts.severity = "MEDIUM";
      conflicts.recommendation =
        "Attention: 1-2 autres employés seront absents. Vérifier la couverture équipe.";
    } else if (overlappingRequests.length <= 5) {
      conflicts.severity = "HIGH";
      conflicts.recommendation =
        "ALERTE: 3-5 employés absents simultanément. Risque de disruption.";
    } else {
      conflicts.severity = "CRITICAL";
      conflicts.recommendation =
        "CRITIQUE: Plus de 5 absences simultanées. À réviser en priorité!";
    }

    return conflicts;
  }

  /**
   * Recommander le meilleur moment pour approuver les demandes
   */
  suggestApprovalOrder(pendingDemandes, allDemandes) {
    const scored = pendingDemandes.map((demande) => {
      let score = 0;

      // Score 1: Urgence (proche date de début)
      const daysUntilStart = moment(demande.date_debut).diff(moment(), "days");
      score += Math.max(0, 100 - daysUntilStart);

      // Score 2: Pas de conflits
      const conflicts = this.detectConflicts(demande, allDemandes);
      score += 100 - conflicts.affectedEmployees * 10;

      // Score 3: Approuvé manager
      if (demande.manager_approval) {
        score += 50;
      }

      // Score 4: Longue demande (peut être approuvée tôt)
      const duration = moment(demande.date_fin).diff(
        moment(demande.date_debut),
        "days",
      );
      if (duration > 5) {
        score += 20;
      }

      return {
        demande,
        score,
        rationale: this._generateApprovalRationale(demande, daysUntilStart),
      };
    });

    return scored.sort((a, b) => b.score - a.score);
  }

  /**
   * Prédire les demandes futures
   */
  predictUpcomingLeaves(employees, historicalDemandes, monthsAhead = 3) {
    const predictions = {};

    employees.forEach((emp) => {
      const empRequests = historicalDemandes.filter(
        (d) => d.employee_id === emp.id && d.statut === "APPROVED",
      );

      // Analyser les patterns
      const months = [];
      empRequests.forEach((req) => {
        const month = moment(req.date_debut).format("YYYY-MM");
        months.push(month);
      });

      // Mois tendance
      const monthCounts = {};
      months.forEach((m) => {
        monthCounts[m] = (monthCounts[m] || 0) + 1;
      });

      const likelyMonths = Object.entries(monthCounts)
        .sort(([, a], [, b]) => b - a)
        .slice(0, 2)
        .map(([month]) => month);

      predictions[emp.id] = {
        employeeName: emp.fullName,
        likelyMonths: likelyMonths,
        avgDaysPerYear:
          empRequests.reduce((sum, d) => sum + (d.nombre_jours || 0), 0) /
          Math.max(empRequests.length, 1),
        frequency: empRequests.length > 0 ? "HIGH" : "LOW",
      };
    });

    return predictions;
  }

  /**
   * Analyser la conformité (vacances nécessaires vs prises)
   */
  analyzeComplianceGaps(employees, demandes, requiredDaysPerYear = 20) {
    const gaps = {};

    employees.forEach((emp) => {
      const approvedDays = demandes
        .filter(
          (d) =>
            d.employee_id === emp.id &&
            d.statut === "APPROVED" &&
            moment(d.date_debut).year() === moment().year(),
        )
        .reduce((sum, d) => sum + (d.nombre_jours || 0), 0);

      const gap = requiredDaysPerYear - approvedDays;

      gaps[emp.id] = {
        name: emp.fullName,
        requiredDays: requiredDaysPerYear,
        approvedDays: approvedDays,
        gap: gap,
        status: gap <= 0 ? "OK" : gap <= 5 ? "WARNING " : "CRITICAL",
        recommendation:
          gap > 0
            ? `L'employé a besoin de ${gap} jours de congés`
            : "Quota atteint",
      };
    });

    return gaps;
  }

  // ============= HELPERS =============

  /**
   * Vérifier si une période contient des week-ends
   */
  _hasWeekendDays(startDate, endDate) {
    for (
      let day = moment(startDate);
      day.isSameOrBefore(endDate);
      day.add(1, "day")
    ) {
      if (day.day() === 0 || day.day() === 6) {
        return true;
      }
    }
    return false;
  }

  /**
   * Générer texte de recommandation
   */
  _generateRecommendationText(startDate, endDate, hasWeekend) {
    let text = `${startDate.format("DD/MM")} au ${endDate.format("DD/MM")} (${
      endDate.diff(startDate, "days") + 1
    } jours)`;
    if (hasWeekend) {
      text += " - Inclut week-end 🎯";
    }
    return text;
  }

  /**
   * Rationale pour approbation
   */
  _generateApprovalRationale(demande, daysUntilStart) {
    if (daysUntilStart < 3) {
      return "Urgent: Demande immédiate";
    } else if (daysUntilStart < 14) {
      return "Priorité: Demande rapide";
    } else if (demande.manager_approval) {
      return "Approuvée manager + délai confortable";
    } else {
      return "Peut attendre validation manager";
    }
  }
}

module.exports = new AIService();
