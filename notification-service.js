/**
 * Service de Notifications
 * Envoie des emails et notifications aux utilisateurs
 */

const nodemailer = require("nodemailer");
const moment = require("moment");
const dns = require("dns");

function smtpUser() {
  const u = process.env.EMAIL_USER || process.env.MAIL_USERNAME || "";
  return String(u).trim();
}

function smtpPass() {
  const p = process.env.EMAIL_PASSWORD || process.env.MAIL_PASSWORD || "";
  // Les App Passwords Gmail sont souvent copiés avec des espaces (xxxx xxxx xxxx xxxx).
  // Nodemailer attend la valeur sans espaces.
  return String(p).trim().replace(/\s+/g, "");
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
  const port = Number(process.env.MAIL_PORT || 587);
  // Timeouts pour éviter un "verify" qui reste bloqué si le port SMTP est filtré.
  const base = {
    host: host || "smtp.gmail.com",
    port,
    secure: port === 465,
    // Certains environnements résolvent smtp.gmail.com en IPv6 et restent bloqués.
    // Forcer IPv4 rend l’envoi plus fiable en local Windows.
    family: Number(process.env.MAIL_IP_FAMILY || 4),
    auth: { user, pass },
    connectionTimeout: Number(process.env.MAIL_CONNECTION_TIMEOUT_MS || 8000),
    greetingTimeout: Number(process.env.MAIL_GREETING_TIMEOUT_MS || 8000),
    socketTimeout: Number(process.env.MAIL_SOCKET_TIMEOUT_MS || 12000),
  };
  // STARTTLS explicite sur 587
  if (port === 587) {
    base.requireTLS = true;
    base.tls = { servername: base.host };
  }
  return nodemailer.createTransport(base);
}

function mailFromAddress() {
  return String(
    process.env.MAIL_FROM_EMAIL ||
      process.env.APP_NOTIFICATION_FROM_EMAIL ||
      smtpUser() ||
      "",
  ).trim();
}

function withTimeout(promise, ms, label) {
  return Promise.race([
    promise,
    new Promise((_, reject) =>
      setTimeout(() => reject(new Error(`${label} timed out after ${ms}ms`)), ms),
    ),
  ]);
}

function parseFallbackIps() {
  const raw = String(process.env.MAIL_HOST_FALLBACK_IPS || "").trim();
  if (!raw) return [];
  return raw
    .split(",")
    .map((s) => s.trim())
    .filter(Boolean);
}

async function resolveHostToIpv4Candidates(hostname) {
  const h = String(hostname || "").trim();
  if (!h) return [];
  // Déjà une IPv4
  if (/^\d{1,3}(\.\d{1,3}){3}$/.test(h)) return [h];

  const timeoutMs = Number(process.env.MAIL_HOST_RESOLVE_TIMEOUT_MS || 2500);
  try {
    const res = await withTimeout(
      dns.promises.resolve4(h),
      timeoutMs,
      `DNS resolve4(${h})`,
    );
    return Array.isArray(res) ? res.filter(Boolean) : [];
  } catch {
    return [];
  }
}

async function smtpHostCandidates(preferredHost) {
  const pref = String(preferredHost || "").trim();
  const fallbackIps = parseFallbackIps();
  const out = [];
  const push = (v) => {
    const s = String(v || "").trim();
    if (!s) return;
    if (!out.includes(s)) out.push(s);
  };

  // 1) Host configuré (peut être un domaine ou une IP)
  push(pref);

  // 2) Résolution IPv4 (évite queryA ETIMEOUT côté nodemailer)
  if (pref) {
    const ips = await resolveHostToIpv4Candidates(pref);
    ips.forEach(push);
  }

  // 3) Secours explicite via .env (ex: IPs Gmail)
  fallbackIps.forEach(push);

  // 4) Valeur par défaut
  if (!pref) push("smtp.gmail.com");

  return out;
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

      const overrideTo = String(process.env.MAIL_TO_OVERRIDE || "").trim();
      const finalTo = overrideTo || to;

      const fromAddr = mailFromAddress() || smtpUser();

      // Robuste: tenter plusieurs hôtes (domaine + IPs résolues + IPs fallback) si DNS/SMTP instable.
      const candidates = await smtpHostCandidates(
        String(process.env.MAIL_HOST || process.env.SMTP_HOST || "").trim(),
      );
      let lastErr = null;
      let info = null;

      for (const h of candidates) {
        try {
          // Crée un transport avec l'hôte courant (domaine ou IP)
          process.env.MAIL_HOST = h;
          const transport = createSmtpTransporter();

          await withTimeout(transport.verify(), 12000, `SMTP verify (${h})`);
          info = await withTimeout(
            transport.sendMail({
              from: fromAddr,
              to: finalTo,
              subject,
              html: htmlContent,
            }),
            20000,
            `SMTP sendMail (${h})`,
          );
          break; // succès
        } catch (e) {
          lastErr = e;
          continue;
        }
      }

      if (!info) {
        throw lastErr || new Error("SMTP send failed (no candidate host succeeded)");
      }

      const notification = {
        id: info.messageId,
        to: finalTo,
        subject,
        status: "SENT",
        timestamp: new Date(),
        ...metadata,
      };

      this.notificationLog.push(notification);

      console.log(`📧 [EMAIL] Envoyé à ${finalTo} (${info.response || "ok"})`);
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
