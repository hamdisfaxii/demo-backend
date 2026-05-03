/**
 * Service de Notifications
 * Envoie des emails et notifications aux utilisateurs
 */

const nodemailer = require("nodemailer");
const moment = require("moment");

function smtpUser() {
  const u = process.env.EMAIL_USER || process.env.MAIL_USERNAME || "";
  return String(u).trim();
}

function smtpPass() {
  const p = process.env.EMAIL_PASSWORD || process.env.MAIL_PASSWORD || "";
  return String(p).trim();
}

function isPlaceholderMailConfig() {
  const u = smtpUser();
  const p = smtpPass();
  if (!u || !p) return true;
  if (u === "votre-email@gmail.com" || u === "your-email@gmail.com") return true;
  if (p === "votre-password" || p === "your-app-password") return true;
  return false;
}

function createSmtpTransporter() {
  const user = smtpUser();
  const pass = smtpPass();
  const host = String(process.env.MAIL_HOST || process.env.SMTP_HOST || "").trim();
  if (host) {
    const port = Number(process.env.MAIL_PORT || 587);
    return nodemailer.createTransport({
      host,
      port,
      secure: port === 465,
      auth: { user, pass },
    });
  }
  return nodemailer.createTransport({
    service: "gmail",
    auth: { user, pass },
  });
}

function mailFromAddress() {
  return String(
    process.env.MAIL_FROM_EMAIL ||
      process.env.APP_NOTIFICATION_FROM_EMAIL ||
      smtpUser() ||
      "",
  ).trim();
}

class NotificationService {
  constructor() {
    this.notificationLog = [];
  }

  /** True si MAIL_* / EMAIL_* permettent un envoi réel (hors placeholders). */
  isSmtpConfigured() {
    return !isPlaceholderMailConfig();
  }

  /**
   * Envoyer notif de création de demande
   */
  async notifyDemandeCreated(demande, user) {
    const subject = `Nouvelle demande de congé créée - ${demande.type_conge}`;
    const htmlContent = `
      <h2>Confirmation de création de demande</h2>
      <p>Bonjour ${user.fullName},</p>
      <p>Votre demande de ${demande.type_conge} a été créée avec succès.</p>
      <div style="background: #f5f5f5; padding: 20px; border-radius: 5px;">
        <p><strong>Détails:</strong></p>
        <p>Du ${demande.date_debut} au ${demande.date_fin}</p>
        <p>Durée: ${demande.nombre_jours || "N/A"} jours</p>
        <p>Statut: En attente d'approbation</p>
      </div>
      <p>Vous serez notifié de toute mise à jour.</p>
      <br>
      <small>Système de Gestion des Congés</small>
    `;

    return this._sendEmail(user.email, subject, htmlContent, {
      demandeId: demande.id,
      event: "DEMANDE_CREATED",
    });
  }

  /**
   * Notifier l'approbation manager
   */
  async notifyManagerApproval(demande, user, manager) {
    const subject = `Demande de congé approuvée par le Manager - ${demande.id}`;
    const htmlContent = `
      <h2>Approbation Manager</h2>
      <p>Bonjour ${user.fullName},</p>
      <p>Votre demande de ${demande.type_conge} a été <strong>approuvée par votre Manager</strong>.</p>
      <p>Approuvé par: ${manager}</p>
      <p>La demande est maintenant en attente d'approbation du département RH.</p>
      <br>
      <small>Système de Gestion des Congés</small>
    `;

    return this._sendEmail(user.email, subject, htmlContent, {
      demandeId: demande.id,
      event: "MANAGER_APPROVED",
    });
  }

  /**
   * Notifier l'approbation RH
   */
  async notifyRHApproval(demande, user) {
    const subject = `Demande APPROUVÉE - ${demande.type_conge}`;
    const htmlContent = `
      <h2 style="color: green;">✓ Demande Approuvée</h2>
      <p>Bonjour ${user.fullName},</p>
      <p>Votre demande de ${demande.type_conge} a été <strong>APPROUVÉE</strong> par le département RH.</p>
      <div style="background: #c8e6c9; padding: 20px; border-radius: 5px; margin: 20px 0;">
        <p><strong>Dates de congé:</strong></p>
        <p>${demande.date_debut} à ${demande.date_fin}</p>
        <p>${demande.nombre_jours} jour(s)</p>
      </div>
      <p><strong>Vos nouveaux soldes:</strong></p>
      <p>Congés restants: Disponible dans votre tableau de bord</p>
      <br>
      <small>Système de Gestion des Congés</small>
    `;

    return this._sendEmail(user.email, subject, htmlContent, {
      demandeId: demande.id,
      event: "RH_APPROVED",
    });
  }

  /**
   * Notifier le rejet
   */
  async notifyRejection(demande, user, reason) {
    const subject = `Demande REJETÉE - ${demande.type_conge}`;
    const htmlContent = `
      <h2 style="color: red;">✗ Demande Rejetée</h2>
      <p>Bonjour ${user.fullName},</p>
      <p>Votre demande de ${demande.type_conge} a malheureusement été <strong>REJETÉE</strong>.</p>
      <p><strong>Raison:</strong></p>
      <p>${reason || "Aucune raison spécifiée"}</p>
      <p>Vous pouvez soumettre une nouvelle demande si nécessaire.</p>
      <br>
      <small>Système de Gestion des Congés</small>
    `;

    return this._sendEmail(user.email, subject, htmlContent, {
      demandeId: demande.id,
      event: "REJECTED",
    });
  }

  /**
   * Notifier RH des demandes en attente
   */
  async notifyRHPendingApproval(count, demandes) {
    const subject = `[URGENT] ${count} demande(s) en attente d'approbation`;
    let demandesList = demandes
      .map(
        (d) =>
          `<li>${d.employee_name} - ${d.type_conge} (${d.date_debut} à ${d.date_fin})</li>`,
      )
      .join("");

    const htmlContent = `
      <h2>Demandes en Attente</h2>
      <p>Il y a <strong>${count} demande(s)</strong> en attente d'approbation RH:</p>
      <ul>
        ${demandesList}
      </ul>
      <p><a href="http://localhost:5174/rh/validation" style="background: #4CAF50; color: white; padding: 10px 20px; text-decoration: none; border-radius: 5px;">Voir les demandes</a></p>
      <br>
      <small>Système de Gestion des Congés</small>
    `;

    // Envoyer à l'email RH (à configurer)
    const rhEmail = process.env.RH_EMAIL || "rh@company.com";
    return this._sendEmail(rhEmail, subject, htmlContent, {
      event: "PENDING_APPROVAL_REMINDER",
    });
  }

  /**
   * Rappel 1 semaine avant congé
   */
  async notifyReminderOneDayBefore(demande, user) {
    const startDate = moment(demande.date_debut).format("DD/MM/YYYY");
    const subject = `Rappel: Vos congés commencent demain!`;
    const htmlContent = `
      <h2>Rappel de Congés</h2>
      <p>Bonjour ${user.fullName},</p>
      <p>Rappel: Vos congés commencent <strong>demain (${startDate})</strong>.</p>
      <p>N'oubliez pas de:</p>
      <ul>
        <li>Vérifier votre boîte aux lettres</li>
        <li>Laisser des contacts d'urgence</li>
        <li>Notifier votre équipe</li>
      </ul>
      <br>
      <small>Système de Gestion des Congés</small>
    `;

    return this._sendEmail(user.email, subject, htmlContent, {
      demandeId: demande.id,
      event: "REMINDER_ONE_DAY_BEFORE",
    });
  }

  /**
   * Fonction interne pour envoyer email
   */
  async _sendEmail(to, subject, htmlContent, metadata = {}) {
    try {
      // Si SMTP non configuré, log seulement (même variables que Spring : MAIL_USERNAME, etc.)
      if (isPlaceholderMailConfig()) {
        const notification = {
          id: `notif_${Date.now()}`,
          to,
          subject,
          status: "LOGGED_ONLY",
          timestamp: new Date(),
          ...metadata,
        };
        this.notificationLog.push(notification);

        console.log(`📧 [NOTIFICATION] Envoi ignoré (email non configuré)`);
        console.log(`   À: ${to}`);
        console.log(`   Sujet: ${subject}`);

        return notification;
      }

      const transport = createSmtpTransporter();
      const fromAddr = mailFromAddress() || smtpUser();
      const info = await transport.sendMail({
        from: fromAddr,
        to,
        subject,
        html: htmlContent,
      });

      const notification = {
        id: info.messageId,
        to,
        subject,
        status: "SENT",
        timestamp: new Date(),
        ...metadata,
      };

      this.notificationLog.push(notification);

      console.log(`📧 [EMAIL] Envoyé à ${to}`);
      return notification;
    } catch (error) {
      console.error(`❌ [EMAIL ERROR]`, error.message);

      const notification = {
        to,
        subject,
        status: "FAILED",
        error: error.message,
        timestamp: new Date(),
        ...metadata,
      };

      this.notificationLog.push(notification);

      return notification;
    }
  }

  /**
   * Obtenir historique des notifications
   */
  getNotificationLog(filters = {}) {
    let log = this.notificationLog;

    if (filters.to) {
      log = log.filter((n) => n.to === filters.to);
    }

    if (filters.status) {
      log = log.filter((n) => n.status === filters.status);
    }

    if (filters.event) {
      log = log.filter((n) => n.event === filters.event);
    }

    return log.slice(-50); // Dernières 50
  }
}

module.exports = new NotificationService();
