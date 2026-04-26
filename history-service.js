/**
 * Service d'Historique et d'Audit
 * Enregistre toutes les actions sur les demandes de congés
 */

const { v4: uuidv4 } = require("uuid");
const moment = require("moment");

class HistoryService {
  constructor() {
    // Stockage en mémoire (en prod, utiliser une DB)
    this.history = [];
  }

  /**
   * Enregistrer une action dans l'historique
   */
  logAction(data) {
    const entry = {
      id: uuidv4(),
      userId: data.userId,
      userName: data.userName,
      action: data.action, // CREATE, UPDATE, APPROVE, REJECT, CANCEL
      entityType: data.entityType || "DEMANDE", // DEMANDE, WORKFLOW, etc
      entityId: data.entityId,
      description: data.description,
      oldValue: data.oldValue || null,
      newValue: data.newValue || null,
      pays: data.pays,
      timestamp: new Date(),
      ipAddress: data.ipAddress || null,
      status: data.status,
    };

    this.history.push(entry);
    console.log(`📝 [HISTORY] ${entry.action}: ${entry.description}`);

    return entry;
  }

  /**
   * Récupérer l'historique avec filtres
   */
  getHistory(filters = {}) {
    let filtered = this.history;

    if (filters.userId) {
      filtered = filtered.filter((h) => h.userId === filters.userId);
    }

    if (filters.action) {
      filtered = filtered.filter((h) => h.action === filters.action);
    }

    if (filters.entityId) {
      filtered = filtered.filter((h) => h.entityId === filters.entityId);
    }

    if (filters.pays) {
      filtered = filtered.filter((h) => h.pays === filters.pays);
    }

    if (filters.startDate && filters.endDate) {
      filtered = filtered.filter(
        (h) =>
          moment(h.timestamp).isSameOrAfter(filters.startDate) &&
          moment(h.timestamp).isSameOrBefore(filters.endDate),
      );
    }

    // Tri par date (plus récent en premier)
    filtered.sort((a, b) => b.timestamp - a.timestamp);

    // Pagination
    const page = filters.page || 1;
    const limit = filters.limit || 50;
    const start = (page - 1) * limit;

    return {
      total: filtered.length,
      page,
      limit,
      data: filtered.slice(start, start + limit),
    };
  }

  /**
   * Obtenir l'historique d'une demande spécifique
   */
  getDemandeHistory(demandeId) {
    return this.history.filter((h) => h.entityId === demandeId);
  }

  /**
   * Statistiques par action
   */
  getStatsByAction() {
    const stats = {};

    this.history.forEach((entry) => {
      if (!stats[entry.action]) {
        stats[entry.action] = {
          count: 0,
          lastOccurrence: null,
        };
      }
      stats[entry.action].count++;
      stats[entry.action].lastOccurrence = entry.timestamp;
    });

    return stats;
  }

  /**
   * Statistiques par utilisateur
   */
  getStatsByUser() {
    const stats = {};

    this.history.forEach((entry) => {
      if (!stats[entry.userId]) {
        stats[entry.userId] = {
          name: entry.userName,
          actionCount: 0,
          actions: {},
        };
      }
      stats[entry.userId].actionCount++;

      if (!stats[entry.userId].actions[entry.action]) {
        stats[entry.userId].actions[entry.action] = 0;
      }
      stats[entry.userId].actions[entry.action]++;
    });

    return stats;
  }

  /**
   * Nettoyer les anciens enregistrements (>90 jours)
   */
  cleanup(daysToKeep = 90) {
    const cutoffDate = moment().subtract(daysToKeep, "days").toDate();
    const originalCount = this.history.length;

    this.history = this.history.filter((h) => h.timestamp > cutoffDate);

    const deletedCount = originalCount - this.history.length;
    console.log(
      `🧹 Historique nettoyé: ${deletedCount} enregistrements supprimés`,
    );

    return deletedCount;
  }

  /**
   * Exporter historique en JSON
   */
  exportJSON() {
    return JSON.stringify(this.history, null, 2);
  }
}

module.exports = new HistoryService();
