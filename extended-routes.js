/**
 * Extensions du Backend - Nouveaux Endpoints
 * À intégrer dans mock-backend.js après les routes existantes
 */

const express = require("express");
const historyService = require("./history-service");
const documentService = require("./document-service");
const notificationService = require("./notification-service");
const analyticsService = require("./analytics-service");
const aiService = require("./ai-service");

function setupExtendedRoutes(
  app,
  demandes,
  users,
  kongesSoldes,
  validPasswords,
) {
  // ============================================
  // HISTORIQUE & AUDIT
  // ============================================

  app.get("/api/history", (req, res) => {
    const filters = {
      userId: req.query.userId,
      action: req.query.action,
      entityId: req.query.entityId,
      pays: req.query.pays,
      startDate: req.query.startDate,
      endDate: req.query.endDate,
      page: parseInt(req.query.page) || 1,
      limit: parseInt(req.query.limit) || 50,
    };

    const result = historyService.getHistory(filters);
    res.json(result);
  });

  app.get("/api/history/:demandeId", (req, res) => {
    const history = historyService.getDemandeHistory(req.params.demandeId);
    res.json(history);
  });

  app.get("/api/history/stats/summary", (req, res) => {
    const stats = {
      byAction: historyService.getStatsByAction(),
      byUser: historyService.getStatsByUser(),
    };
    res.json(stats);
  });

  // ============================================
  // DOCUMENTS (PDF / EXCEL)
  // ============================================

  app.get("/api/demande/:id/pdf", async (req, res) => {
    try {
      const demande = demandes.find((d) => d.id === req.params.id);
      if (!demande) {
        return res.status(404).json({ error: "Demande not found" });
      }

      const user = users[demande.employee_id];
      if (!user) {
        return res.status(404).json({ error: "User not found" });
      }

      const result = await documentService.generateDemandeAttestation(
        demande,
        user,
      );

      res.download(result.filePath, result.fileName);
    } catch (error) {
      console.error("❌ PDF Generation Error:", error);
      res.status(500).json({ error: "Failed to generate PDF" });
    }
  });

  app.get("/api/demande/export/excel", async (req, res) => {
    try {
      const filter = req.query.filter || "all";
      let demandesFiltered = demandes;

      if (filter === "approved") {
        demandesFiltered = demandes.filter((d) => d.statut === "APPROVED");
      } else if (filter === "pending") {
        demandesFiltered = demandes.filter((d) => d.statut === "PENDING");
      } else if (filter === "rejected") {
        demandesFiltered = demandes.filter((d) => d.statut === "REJECTED");
      }

      const result = await documentService.generateRHReport(demandesFiltered);

      res.download(result.filePath, result.fileName);
    } catch (error) {
      console.error("❌ Excel Export Error:", error);
      res.status(500).json({ error: "Failed to export Excel" });
    }
  });

  app.get("/api/documents/list", (req, res) => {
    const documents = documentService.listDocuments();
    res.json(documents);
  });

  // ============================================
  // NOTIFICATIONS
  // ============================================

  app.post("/api/notifications/test", async (req, res) => {
    const { email, type } = req.body;

    try {
      let result;
      const testUser = { email, fullName: "Test User" };
      const testDemande = {
        id: "TEST-001",
        type_conge: "CONGES_PAYES",
        date_debut: "2026-05-01",
        date_fin: "2026-05-05",
        nombre_jours: 5,
      };

      if (type === "created") {
        result = await notificationService.notifyDemandeCreated(
          testDemande,
          testUser,
        );
      } else if (type === "approved") {
        result = await notificationService.notifyRHApproval(
          testDemande,
          testUser,
        );
      } else if (type === "rejected") {
        result = await notificationService.notifyRejection(
          testDemande,
          testUser,
          "Test rejection",
        );
      }

      res.json({ success: true, notification: result });
    } catch (error) {
      res.status(500).json({ error: error.message });
    }
  });

  app.get("/api/notifications/log", (req, res) => {
    const filters = {
      to: req.query.to,
      status: req.query.status,
      event: req.query.event,
    };

    const log = notificationService.getNotificationLog(filters);
    res.json(log);
  });

  // ============================================
  // ANALYTICS & DASHBOARD RH
  // ============================================

  app.get("/api/rh/analytics/dashboard", (req, res) => {
    const dateRange = {
      start: req.query.startDate,
      end: req.query.endDate,
    };

    const dashboard = analyticsService.generateCompleteDashboard(
      demandes,
      dateRange,
    );

    res.json(dashboard);
  });

  app.get("/api/rh/analytics/global-stats", (req, res) => {
    const stats = analyticsService.calculateGlobalStats(demandes);
    res.json(stats);
  });

  app.get("/api/rh/analytics/by-type", (req, res) => {
    const stats = analyticsService.calculateByLeaveType(demandes);
    res.json(stats);
  });

  app.get("/api/rh/analytics/by-country", (req, res) => {
    const stats = analyticsService.calculateByCountry(demandes);
    res.json(stats);
  });

  app.get("/api/rh/analytics/top-absent", (req, res) => {
    const limit = parseInt(req.query.limit) || 10;
    const topAbsent = analyticsService.calculateTopAbsentEmployees(
      demandes,
      limit,
    );
    res.json(topAbsent);
  });

  app.get("/api/rh/analytics/monthly-trends", (req, res) => {
    const months = parseInt(req.query.months) || 12;
    const trends = analyticsService.calculateMonthlyTrends(demandes, months);
    res.json(trends);
  });

  app.get("/api/rh/analytics/critical-days", (req, res) => {
    const criticalDays = analyticsService.calculateCriticalDays(demandes);
    res.json(criticalDays);
  });

  app.get("/api/rh/analytics/employee/:userId", (req, res) => {
    const employees = analyticsService.calculateByEmployee(demandes);
    const empData = employees[req.params.userId];

    if (!empData) {
      return res.status(404).json({ error: "Employee not found" });
    }

    res.json(empData);
  });

  // ============================================
  // IA & SUGGESTIONS INTELLIGENTES
  // ============================================

  app.post("/api/ai/suggest-dates", (req, res) => {
    const { employeeId, leaveType, duration } = req.body;

    if (!employeeId || !leaveType || !duration) {
      return res.status(400).json({ error: "Missing required parameters" });
    }

    const employee = users[employeeId];
    if (!employee) {
      return res.status(404).json({ error: "Employee not found" });
    }

    const suggestions = aiService.suggestOptimalDates(
      employee,
      leaveType,
      duration,
      demandes,
    );

    res.json({
      employeeName: employee.fullName,
      leaveType,
      requestedDuration: duration,
      suggestions,
    });
  });

  app.post("/api/ai/detect-conflicts", (req, res) => {
    const { demandeId } = req.body;

    const demande = demandes.find((d) => d.id === demandeId);
    if (!demande) {
      return res.status(404).json({ error: "Demande not found" });
    }

    const conflicts = aiService.detectConflicts(demande, demandes, users);

    res.json(conflicts);
  });

  app.get("/api/ai/approval-order", (req, res) => {
    const pending = demandes.filter((d) => d.statut === "PENDING");
    const ordered = aiService.suggestApprovalOrder(pending, demandes);

    res.json({
      totalPending: pending.length,
      suggestedOrder: ordered.map((item) => ({
        demandeId: item.demande.id,
        employeeName: item.demande.employee_name,
        leaveType: item.demande.type_conge,
        dates: `${item.demande.date_debut} à ${item.demande.date_fin}`,
        score: item.score,
        rationale: item.rationale,
      })),
    });
  });

  app.get("/api/ai/compliance-gaps", (req, res) => {
    const userList = Object.values(users);
    const gaps = aiService.analyzeComplianceGaps(userList, demandes);

    res.json({
      requiredDaysPerYear: 20,
      gaps: Object.values(gaps),
      criticalsCount: Object.values(gaps).filter((g) => g.status === "CRITICAL")
        .length,
    });
  });

  app.get("/api/ai/predictions", (req, res) => {
    const userList = Object.values(users);
    const predictions = aiService.predictUpcomingLeaves(userList, demandes);

    res.json(predictions);
  });

  // ============================================
  // ROUTES UTILITAIRES
  // ============================================

  app.get("/api/health/extended", (req, res) => {
    res.json({
      status: "✅ All extensions loaded",
      services: {
        history: "✅",
        documents: "✅",
        notifications: "✅",
        analytics: "✅",
        ai: "✅",
      },
      uptime: process.uptime(),
      memory: {
        demandes: demandes.length,
        history: historyService.history.length,
        documents: documentService.listDocuments().length,
      },
    });
  });

  console.log(`
  ✅ EXTENSIONS CHARGÉES:
     • Historique & Audit
     • Documents (PDF/Excel)
     • Notifications (Email)
     • Analytics & Dashboard RH
     • IA & Suggestions
  `);
}

module.exports = { setupExtendedRoutes };
