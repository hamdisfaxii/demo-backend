/**
 * Service d'Analytics pour le Dashboard RH
 * Calcule les statistiques et métriques RH
 */

const moment = require("moment");

class AnalyticsService {
  /**
   * Calculer statistiques globales
   */
  calculateGlobalStats(demandes) {
    const stats = {
      total: demandes.length,
      approved: 0,
      rejected: 0,
      pending: 0,
      cancelled: 0,
      totalDaysRequested: 0,
      totalDaysApproved: 0,
    };

    demandes.forEach((d) => {
      if (d.statut === "APPROVED") {
        stats.approved++;
        stats.totalDaysApproved += d.nombre_jours || 0;
      } else if (d.statut === "REJECTED") {
        stats.rejected++;
      } else if (d.statut === "PENDING") {
        stats.pending++;
        stats.totalDaysRequested += d.nombre_jours || 0;
      } else if (d.statut === "CANCELLED") {
        stats.cancelled++;
      }
    });

    stats.approvalRate =
      stats.total > 0
        ? Math.round(((stats.approved + stats.rejected) / stats.total) * 100)
        : 0;

    return stats;
  }

  /**
   * Statistiques par type de congé
   */
  calculateByLeaveType(demandes) {
    const stats = {};

    demandes.forEach((d) => {
      const type = d.type_conge;
      if (!stats[type]) {
        stats[type] = {
          requested: 0,
          approved: 0,
          rejected: 0,
          pending: 0,
          daysRequested: 0,
          daysApproved: 0,
        };
      }

      stats[type].requested++;
      stats[type].daysRequested += d.nombre_jours || 0;

      if (d.statut === "APPROVED") {
        stats[type].approved++;
        stats[type].daysApproved += d.nombre_jours || 0;
      } else if (d.statut === "REJECTED") {
        stats[type].rejected++;
      } else if (d.statut === "PENDING") {
        stats[type].pending++;
      }
    });

    return stats;
  }

  /**
   * Statistiques par pays
   */
  calculateByCountry(demandes) {
    const stats = {};

    demandes.forEach((d) => {
      const country = d.pays || "UNKNOWN";
      if (!stats[country]) {
        stats[country] = {
          total: 0,
          approved: 0,
          rejected: 0,
          pending: 0,
          employees: new Set(),
          avgDaysPerRequest: 0,
        };
      }

      stats[country].total++;
      stats[country].employees.add(d.employee_id);

      if (d.statut === "APPROVED") {
        stats[country].approved++;
      } else if (d.statut === "REJECTED") {
        stats[country].rejected++;
      } else if (d.statut === "PENDING") {
        stats[country].pending++;
      }

      stats[country].avgDaysPerRequest =
        (stats[country].avgDaysPerRequest +
          (d.nombre_jours || 0 / stats[country].total)) /
        2;
    });

    // Convertir Set en nombre
    Object.keys(stats).forEach((country) => {
      stats[country].employeeCount = stats[country].employees.size;
      delete stats[country].employees;
    });

    return stats;
  }

  /**
   * Employés les plus absents
   */
  calculateTopAbsentEmployees(demandes, limit = 10) {
    const employeeStats = {};

    demandes.forEach((d) => {
      if (d.statut === "APPROVED") {
        if (!employeeStats[d.employee_id]) {
          employeeStats[d.employee_id] = {
            name: d.employee_name,
            totalDays: 0,
            requestsCount: 0,
            lastLeave: null,
          };
        }

        employeeStats[d.employee_id].totalDays += d.nombre_jours || 0;
        employeeStats[d.employee_id].requestsCount++;
        employeeStats[d.employee_id].lastLeave =
          d.date_fin > employeeStats[d.employee_id].lastLeave
            ? d.date_fin
            : employeeStats[d.employee_id].lastLeave;
      }
    });

    return Object.values(employeeStats)
      .sort((a, b) => b.totalDays - a.totalDays)
      .slice(0, limit);
  }

  /**
   * Tendances mensuelles
   */
  calculateMonthlyTrends(demandes, monthsBack = 12) {
    const trends = {};
    const now = moment();

    for (let i = monthsBack - 1; i >= 0; i--) {
      const date = moment(now).subtract(i, "months");
      const monthKey = date.format("YYYY-MM");
      trends[monthKey] = { requested: 0, approved: 0, rejected: 0 };
    }

    demandes.forEach((d) => {
      const monthKey = moment(d.date_debut).format("YYYY-MM");
      if (trends[monthKey]) {
        trends[monthKey].requested++;
        if (d.statut === "APPROVED") {
          trends[monthKey].approved++;
        } else if (d.statut === "REJECTED") {
          trends[monthKey].rejected++;
        }
      }
    });

    return trends;
  }

  /**
   * Détails par employé
   */
  calculateByEmployee(demandes) {
    const employees = {};

    demandes.forEach((d) => {
      if (!employees[d.employee_id]) {
        employees[d.employee_id] = {
          name: d.employee_name,
          email: d.employee_email || "N/A",
          totalRequests: 0,
          approvedDays: 0,
          pendingDays: 0,
          rejectedCount: 0,
          lastRequestDate: null,
        };
      }

      employees[d.employee_id].totalRequests++;

      if (d.statut === "APPROVED") {
        employees[d.employee_id].approvedDays += d.nombre_jours || 0;
      } else if (d.statut === "PENDING") {
        employees[d.employee_id].pendingDays += d.nombre_jours || 0;
      } else if (d.statut === "REJECTED") {
        employees[d.employee_id].rejectedCount++;
      }

      employees[d.employee_id].lastRequestDate =
        d.date_debut > employees[d.employee_id].lastRequestDate
          ? d.date_debut
          : employees[d.employee_id].lastRequestDate;
    });

    return employees;
  }

  /**
   * Heatmap: Jours critiques (pics d'absences)
   */
  calculateCriticalDays(demandes) {
    const dayCount = {};

    demandes.forEach((d) => {
      if (d.statut === "APPROVED") {
        const start = moment(d.date_debut);
        const end = moment(d.date_fin);

        for (
          let day = moment(start);
          day.isSameOrBefore(end);
          day.add(1, "day")
        ) {
          const dateKey = day.format("YYYY-MM-DD");
          dayCount[dateKey] = (dayCount[dateKey] || 0) + 1;
        }
      }
    });

    // Trouver les jours les plus critiques
    const sorted = Object.entries(dayCount)
      .sort(([, a], [, b]) => b - a)
      .slice(0, 20);

    return {
      criticalDays: sorted.map(([date, count]) => ({
        date,
        absentCount: count,
      })),
      maxAbsentOnSingleDay: Math.max(...Object.values(dayCount), 0),
    };
  }

  /**
   * Prévisions (simple)
   */
  calculateForecasts(demandes) {
    const approved = demandes.filter((d) => d.statut === "APPROVED");
    const avgDaysPerRequest =
      approved.length > 0
        ? approved.reduce((sum, d) => sum + (d.nombre_jours || 0), 0) /
          approved.length
        : 0;

    const pending = demandes.filter((d) => d.statut === "PENDING");
    const estimatedApprovedDays = pending.length * avgDaysPerRequest;

    return {
      avgDaysPerRequest: Math.round(avgDaysPerRequest * 100) / 100,
      pendingRequests: pending.length,
      estimatedApprovedDays: Math.round(estimatedApprovedDays),
      projectedTotalDays:
        approved.reduce((sum, d) => sum + (d.nombre_jours || 0), 0) +
        estimatedApprovedDays,
    };
  }

  /**
   * Dashboard complet
   */
  generateCompleteDashboard(demandes, dateRange = {}) {
    // Filtrer par période si spécifiée
    let filteredDemandes = demandes;
    if (dateRange.start && dateRange.end) {
      filteredDemandes = demandes.filter(
        (d) =>
          moment(d.date_debut).isSameOrAfter(dateRange.start) &&
          moment(d.date_fin).isSameOrBefore(dateRange.end),
      );
    }

    return {
      timestamp: new Date(),
      dateRange,
      globalStats: this.calculateGlobalStats(filteredDemandes),
      byLeaveType: this.calculateByLeaveType(filteredDemandes),
      byCountry: this.calculateByCountry(filteredDemandes),
      topAbsentEmployees: this.calculateTopAbsentEmployees(filteredDemandes),
      monthlyTrends: this.calculateMonthlyTrends(filteredDemandes),
      criticalDays: this.calculateCriticalDays(filteredDemandes),
      forecasts: this.calculateForecasts(filteredDemandes),
      byEmployee: this.calculateByEmployee(filteredDemandes),
    };
  }
}

module.exports = new AnalyticsService();
