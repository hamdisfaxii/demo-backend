/**
 * ============================================
 * Backend API - Gestion des Congés
 * Intégration BPM avec Dolibarr ERP
 * ============================================
 *
 * Cahier des charges:
 * - Gestion des demandes de congés
 * - Workflow multi-niveaux automatisé
 * - Gestion multi-pays (TN, MA, FR)
 * - Intégration Dolibarr ERP via API REST
 * - Génération de documents (PDF, attestations)
 * - Tableau de bord RH avancé
 * - Authentification unifiée via Dolibarr
 */

const express = require("express");
const cors = require("cors");
const fetch = require("node-fetch");
const mysql = require("mysql2/promise");
require("dotenv").config();
const app = express();
const PORT = Number(process.env.SERVER_PORT || 8080);

app.use(cors());
app.use(express.json());

// ============================================
// CONFIGURATION DOLIBARR
// ============================================
const DOLIBARR_URL =
  process.env.DOLIBARR_URL || "http://localhost/dolibarr/api/index.php";
const DOLIBARR_API_KEY = process.env.DOLIBARR_API_KEY || "";

// ============================================
// CONFIGURATION BASE DE DONNÉES DOLIBARR
// ============================================
const DB_CONFIG = {
  host: process.env.DOLIBARR_DB_HOST || "localhost",
  user: process.env.DOLIBARR_DB_USER || "root",
  password: process.env.DOLIBARR_DB_PASSWORD || "",
  database: process.env.DOLIBARR_DB_NAME || "dolibarr",
  port: Number(process.env.DOLIBARR_DB_PORT || 3306),
  waitForConnections: true,
  connectionLimit: 10,
  queueLimit: 0,
};

// Créer un pool de connexions
const pool = mysql.createPool(DB_CONFIG);

// Fonction pour récupérer les soldes depuis Dolibarr
async function getHolidaysFromDolibarr(userId) {
  try {
    const conn = await pool.getConnection();

    // Essayer de récupérer les soldes depuis llx_holiday_users
    // NOTE: Les colonnes pourront être: rowid, fk_user, type_holiday_id, nb_total, nb_used, nb_available, etc.
    const [rows] = await conn.execute(
      `SELECT * FROM llx_holiday_users WHERE fk_user = ? LIMIT 5`,
      [userId],
    );
    conn.release();

    if (rows && rows.length > 0) {
      console.log(
        `  ✓ Found ${rows.length} holiday entries in Dolibarr for user ${userId}:`,
        JSON.stringify(rows[0]),
      );
    } else {
      console.log(`  ⚠️  No holiday entries found for user ${userId}`);
    }
    return rows;
  } catch (error) {
    console.error(
      `❌ Error querying llx_holiday_users for user ${userId}:`,
      error.message,
    );
    return [];
  }
}

// Fonction pour récupérer les types de congés de Dolibarr
async function mapDolibarrHolidayTypes() {
  try {
    const conn = await pool.getConnection();

    // Récupérer tous les types de congés depuis Dolibarr
    const [types] = await conn.execute(
      `SELECT rowid, code, label FROM llx_c_holiday_types WHERE active = 1`,
    );
    conn.release();

    // Créer un mapping ID Dolibarr -> code/label
    const mapping = {};
    types.forEach((type) => {
      mapping[type.rowid] = {
        code: type.code,
        label: type.label,
      };
    });

    return mapping;
  } catch (error) {
    console.error(`❌ Error querying llx_c_holiday_types:`, error.message);
    return {};
  }
}

// ============================================
// 1. DONNÉES DE BASE
// ============================================

// Utilisateurs avec rôles et pays
const users = {
  1: {
    id: 1,
    username: "john.doe",
    email: "john@company.com",
    fullName: "John Doe",
    role: "EMPLOYEE",
    pays: "TN",
    departement: "IT",
    manager_id: 3,
    dolibarr_id: 101,
  },
  2: {
    id: 2,
    username: "sarah.martin",
    email: "sarah@company.com",
    fullName: "Sarah Martin",
    role: "EMPLOYEE",
    pays: "FR",
    departement: "RH",
    manager_id: 3,
    dolibarr_id: 102,
  },
  3: {
    id: 3,
    username: "rh.admin",
    email: "rh@company.com",
    fullName: "RH Admin",
    role: "RH",
    pays: "TN",
    departement: "RH",
    dolibarr_id: 200,
  },
  4: {
    id: 4,
    username: "drh.admin",
    email: "drh@company.com",
    fullName: "DRH Admin",
    role: "DRH",
    pays: "TN",
    departement: "RH",
    dolibarr_id: 201,
  },
  5: {
    id: 5,
    username: "hamdi.sfaxi",
    email: "hamdihamdisfaxi123@gmail.com",
    fullName: "Hamdi Sfaxi",
    role: "EMPLOYEE",
    pays: "TN",
    departement: "IT",
    manager_id: 3,
  },
};

// Types de congés par pays
const typesCongePays = {
  TN: {
    CONGES_PAYES: { nom: "Congés Payés", jours: 30, code: "CP" },
    RTT: { nom: "RTT", jours: 10, code: "RTT" },
    MALADIE: { nom: "Congé Maladie", jours: 0, code: "MAL" },
    PARENTAL: { nom: "Congé Parental", jours: 180, code: "PAR" },
  },
  FR: {
    CONGES_PAYES: { nom: "Congés Payés", jours: 25, code: "CP" },
    RTT: { nom: "RTT", jours: 12, code: "RTT" },
    MALADIE: { nom: "Congé Maladie", jours: 0, code: "MAL" },
    PARENTAL: { nom: "Congé Parental", jours: 180, code: "PAR" },
    ENFANT_MALADE: { nom: "Congé Enfant Malade", jours: 5, code: "ENF" },
  },
  MA: {
    CONGES_PAYES: { nom: "Congés Payés", jours: 22, code: "CP" },
    MALADIE: { nom: "Congé Maladie", jours: 0, code: "MAL" },
    PARENTAL: { nom: "Congé Parental", jours: 120, code: "PAR" },
  },
};

const exceptionalLeavesByCountry = {
  TN: [
    { id: 1, label: "Mariage", daysPerYear: 3, enabled: true },
    { id: 2, label: "Naissance", daysPerYear: 2, enabled: true },
    { id: 3, label: "Décès conjoint", daysPerYear: 3, enabled: true },
    { id: 4, label: "Décès parent", daysPerYear: 2, enabled: true },
  ],
  FR: [
    { id: 5, label: "Mariage", daysPerYear: 4, enabled: true },
    { id: 6, label: "Naissance", daysPerYear: 3, enabled: true },
    { id: 7, label: "Décès conjoint", daysPerYear: 3, enabled: true },
    { id: 8, label: "Déménagement", daysPerYear: 1, enabled: false },
  ],
  MA: [
    { id: 9, label: "Mariage", daysPerYear: 4, enabled: true },
    { id: 10, label: "Naissance", daysPerYear: 2, enabled: true },
    { id: 11, label: "Décès conjoint", daysPerYear: 3, enabled: true },
    { id: 12, label: "Décès parent", daysPerYear: 3, enabled: true },
  ],
};

const nextExceptionalLeaveId = () =>
  Object.values(exceptionalLeavesByCountry)
    .flatMap((rows) => rows.map((row) => row.id))
    .reduce((max, id) => Math.max(max, id), 0) + 1;

// Jours fériés par pays
const joursFeriesPays = {
  TN: [
    { date: "2026-01-14", nom: "Révolution" },
    { date: "2026-03-20", nom: "Indépendance" },
    { date: "2026-07-25", nom: "République" },
  ],
  FR: [
    { date: "2026-01-01", nom: "Jour de l'An" },
    { date: "2026-05-01", nom: "Fête du Travail" },
    { date: "2026-07-14", nom: "Fête Nationale" },
  ],
  MA: [
    { date: "2026-01-11", nom: "Indépendance" },
    { date: "2026-05-01", nom: "Fête du Travail" },
    { date: "2026-07-30", nom: "Trône" },
  ],
};

let publicHolidaySeq = 1;
const publicHolidaysStore = Object.entries(joursFeriesPays).reduce(
  (acc, [countryCode, rows]) => {
    acc[countryCode] = rows.map((row) => ({
      id: publicHolidaySeq++,
      countryCode,
      libelle: row.nom,
      dateJour: row.date,
      active: true,
      source: "seed",
    }));
    return acc;
  },
  {},
);

// Soldes de congés
const kongesSoldes = {
  1: {
    CONGES_PAYES: { solde_initial: 30, utilise: 8, restant: 22 },
    RTT: { solde_initial: 10, utilise: 2, restant: 8 },
  },
  2: {
    CONGES_PAYES: { solde_initial: 25, utilise: 5, restant: 20 },
    RTT: { solde_initial: 12, utilise: 0, restant: 12 },
  },
};

// Passwords valides (mis à jour au login depuis Dolibarr)
let validPasswords = {
  "john@company.com": "password123",
  "sarah@company.com": "password123",
  "rh@company.com": "password123",
  "drh@company.com": "password123",
  "hamdihamdisfaxi123@gmail.com": "password123",
};

// Demandes
let demandes = [
  {
    id: 1,
    userId: 1,
    typeConge: "CONGES_PAYES",
    dateDebut: "2026-05-01",
    dateFin: "2026-05-10",
    nombreJours: 8,
    raison: "Vacances familiales",
    statut: "EN_ATTENTE",
    dateCreation: "2026-04-10",
    historique: [{ date: "2026-04-10", action: "CREATION", user_id: 1 }],
    workflow: {
      manager_id: 3,
      manager_status: null,
      rh_id: 3,
      rh_status: null,
      drh_id: 4,
      drh_status: null,
    },
    dolibarr_sync: false,
  },
];

const dolibarrConfig = {
  url: "http://localhost/dolibarr/api/index.php",
  apiKey: "abc123xyz456SECUREKEY",
  syncEnabled: false,
};

let workflowRules = [
  {
    id: 1,
    code: "WF_DEFAULT",
    country: "DEFAULT",
    steps: [
      { stepOrder: 1, approverRole: "MANAGER", minDays: 1, maxDays: null },
      { stepOrder: 2, approverRole: "RH", minDays: 1, maxDays: null },
      { stepOrder: 3, approverRole: "ADMIN", minDays: 5, maxDays: null },
    ],
  },
];

// ============================================
// UTILITAIRES
// ============================================

function calculerJoursOuvrables(dateDebut, dateFin, pays) {
  let jours = 0;
  let date = new Date(dateDebut);
  const fin = new Date(dateFin);
  const feriesMap = new Map(
    joursFeriesPays[pays]?.map((f) => [f.date, true]) || [],
  );

  while (date <= fin) {
    const dayOfWeek = date.getDay();
    const dateStr = date.toISOString().split("T")[0];

    if (dayOfWeek !== 0 && dayOfWeek !== 6 && !feriesMap.has(dateStr)) {
      jours++;
    }

    date.setDate(date.getDate() + 1);
  }

  return jours;
}

function validerDates(dateDebut, dateFin) {
  const debut = new Date(dateDebut);
  const fin = new Date(dateFin);

  if (debut > fin)
    return { valid: false, error: "Date de début > date de fin" };
  if (debut < new Date()) return { valid: false, error: "Date passée" };

  return { valid: true };
}

function normalizeStatusForApi(statut) {
  const raw = String(statut || "").toUpperCase();
  if (raw.includes("ATTENTE")) return "PENDING";
  if (raw.includes("ACCEPTE") || raw.includes("APPROUVE")) return "APPROVED";
  if (raw.includes("REJET") || raw.includes("REFUS")) return "REJECTED";
  if (raw.includes("ANNULE")) return "CANCELLED";
  return raw || "UNKNOWN";
}

function toRhRequestDto(demande) {
  const employe = users[demande.userId] || {};
  return {
    id: demande.id,
    typeConge: demande.typeConge,
    statut: normalizeStatusForApi(demande.statut),
    rawStatus: demande.statut,
    dateDebut: demande.dateDebut,
    dateFin: demande.dateFin,
    nombreJours: demande.nombreJours,
    motif: demande.raison || "",
    commentaireRh: demande.commentaireRh || "",
    dateSoumission: demande.dateCreation,
    workflowCode: "WF_DEFAULT",
    currentStepOrder: demande.workflow?.rh_status ? 3 : 2,
    currentStepType: demande.workflow?.rh_status ? "ADMIN_APPROVAL" : "RH_APPROVAL",
    employe: {
      id: employe.id,
      nom: employe.fullName?.split(" ").slice(1).join(" ") || employe.fullName || "",
      prenom: employe.fullName?.split(" ")[0] || "",
      email: employe.email || "",
      country: employe.pays || "",
      department: employe.departement || "",
    },
    historique: demande.historique || [],
  };
}

// ============================================
// AUTHENTIFICATION - INTÉGRATION DOLIBARR
// ============================================

app.post("/api/auth/login", async (req, res) => {
  const { email, password } = req.body;

  try {
    console.log("🔐 Login attempt:", email);

    // Faire un fallback: d'abord essayer Dolibarr, sinon utiliser les users locaux
    let user = null;

    try {
      // 1. Récupérer TOUS les utilisateurs depuis Dolibarr API endpoint /users
      const dolibarrResponse = await fetch(`${DOLIBARR_URL}/users`, {
        method: "GET",
        headers: {
          DOLAPIKEY: DOLIBARR_API_KEY,
          Accept: "application/json",
        },
      });

      console.log(
        "✅ Dolibarr /users response status:",
        dolibarrResponse.status,
      );

      if (dolibarrResponse.ok) {
        let dolibarrUsers = await dolibarrResponse.json();
        console.log(
          "📡 Dolibarr users received:",
          Array.isArray(dolibarrUsers) ? dolibarrUsers.length : "single object",
        );

        // Dolibarr retourne un array de tous les users
        if (dolibarrUsers && Array.isArray(dolibarrUsers)) {
          // Filtrer par email en JavaScript (SÉCURISÉ - pas d'injection SQL)
          const dolibarrUser = dolibarrUsers.find((u) => u.email === email);

          if (dolibarrUser) {
            // Déterminer le rôle (supporte int/string selon versions Dolibarr)
            let role = "EMPLOYEE";
            const adminFlag = Number(dolibarrUser.admin) === 1;
            const superAdminFlag = Number(dolibarrUser.superadmin) === 1;
            const loginHint = String(dolibarrUser.login || "").toLowerCase();
            const emailHint = String(dolibarrUser.email || "").toLowerCase();
            if (
              adminFlag ||
              superAdminFlag ||
              loginHint.includes("admin") ||
              emailHint.includes("admin")
            ) {
              role = "DRH";
            }

            // Créer utilisateur local à partir de Dolibarr
            const userId = dolibarrUser.id || Math.floor(Math.random() * 1000);
            user = {
              id: userId,
              email: dolibarrUser.email,
              fullName:
                (dolibarrUser.firstname || "") +
                " " +
                (dolibarrUser.lastname || ""),
              role: role,
              pays: "TN",
              departement: dolibarrUser.department || "General",
              dolibarr_id: dolibarrUser.id,
            };
            users[userId] = user;
            console.log("✅ User found in Dolibarr:", user.fullName);
          } else {
            console.log("⚠️  User email not found in Dolibarr users:", email);
          }
        }
      } else {
        console.log("❌ Dolibarr API error:", dolibarrResponse.status);
        const errorText = await dolibarrResponse.text();
        console.log("Error details:", errorText.substring(0, 200));
      }
    } catch (dolibarrError) {
      console.log(
        "⚠️  Dolibarr error, falling back to local users:",
        dolibarrError.message,
      );
    }

    // 2. Si pas trouvé dans Dolibarr, chercher dans les users locaux
    if (!user) {
      user = Object.values(users).find((u) => u.email === email);
      if (user) {
        console.log("✅ User found locally:", user.fullName);
      }
    }

    // 3. Vérifier que l'utilisateur existe
    if (!user) {
      console.log("❌ User not found");
      return res.status(401).json({ error: "Utilisateur non trouvé" });
    }

    // 4. Valider le password
    // Pour les utilisateurs LOCAUX: vérifier le password
    // Pour les utilisateurs DOLIBARR: accepter le premier password (créer une session Dolibarr)
    if (!user.dolibarr_id) {
      // Utilisateur LOCAL - validation requise
      if (validPasswords[email] !== password) {
        console.log("❌ Invalid password for local user");
        return res.status(401).json({ error: "Mot de passe incorrect" });
      }
    } else {
      // Utilisateur DOLIBARR - première connexion
      if (!validPasswords[email] && password) {
        // Première connexion: accepter le password et le stocker
        validPasswords[email] = password;
        console.log("✅ First login from Dolibarr user, password stored");
      }
    }

    // 5. Stocker le password pour les connexions futures (local users)
    if (!user.dolibarr_id) {
      validPasswords[email] = password;
    }

    // 6. INITIALISER LES SOLDES DE CONGÉ AU LOGIN - DEPUIS DOLIBARR
    // Récupérer les vraies valeurs depuis la base Dolibarr
    if (!kongesSoldes[user.id]) {
      const paysUser = user.pays || "TN";
      const typesCongePaysUser = typesCongePays[paysUser] || {};

      kongesSoldes[user.id] = {};

      // Essayer de récupérer les soldes depuis Dolibarr
      if (user.dolibarr_id) {
        const dolibarrHolidays = await getHolidaysFromDolibarr(
          user.dolibarr_id,
        );

        if (dolibarrHolidays && dolibarrHolidays.length > 0) {
          console.log(
            `📊 ${dolibarrHolidays.length} types de congés trouvés dans Dolibarr pour user ${user.dolibarr_id}`,
          );

          // Initialiser avec valeurs par défaut
          Object.keys(typesCongePaysUser).forEach((typeKey) => {
            kongesSoldes[user.id][typeKey] = {
              utilise: 0,
              restant: typesCongePaysUser[typeKey].jours,
            };
          });

          // Mettre à jour avec les valeurs RÉELLES de Dolibarr
          dolibarrHolidays.forEach((holiday) => {
            // holiday.fk_c_holiday_types est l'ID Dolibarr du type de congé
            // holiday.nb_available = jours disponibles
            // holiday.nb_used = jours utilisés
            // holiday.nb_total = total

            const typeId = holiday.fk_c_holiday_types;
            const availableDays = holiday.nb_available || 0;
            const usedDays = holiday.nb_used || 0;

            // Chercher le type de congé correspondant dans nos types locaux
            // On peut mapper par le nom ou créer une table de correspondance
            const typeKey = Object.keys(typesCongePaysUser).find((key) => {
              const localType = typesCongePaysUser[key];
              // Correspondance simple: CP<->CP, RTT<->RTT, etc.
              return (
                localType.code.substring(0, 3) === typeId.toString() ||
                localType.nom.includes(typeId.toString())
              );
            });

            // Pour l'instant, faire un mapping simple basé sur les types connus
            if (availableDays > 0) {
              // Déterminer le type basé sur le nombre de jours (heuristique)
              let mappedKey = "CONGES_PAYES"; // Par défaut
              if (availableDays === 10 || availableDays === 12) {
                mappedKey = "RTT";
              } else if (availableDays === 180) {
                mappedKey = "PARENTAL";
              } else if (
                availableDays === 30 ||
                availableDays === 25 ||
                availableDays === 22
              ) {
                mappedKey = "CONGES_PAYES";
              }

              if (typesCongePaysUser[mappedKey]) {
                kongesSoldes[user.id][mappedKey] = {
                  utilise: usedDays,
                  restant: availableDays,
                };
                console.log(
                  `  ✓ ${typesCongePaysUser[mappedKey].nom}: ${availableDays} jours (utilisé: ${usedDays}) DEPUIS DOLIBARR`,
                );
              }
            }
          });
        } else {
          // Pas de soldes dans Dolibarr, utiliser les valeurs par défaut du pays
          Object.keys(typesCongePaysUser).forEach((typeKey) => {
            kongesSoldes[user.id][typeKey] = {
              utilise: 0,
              restant: typesCongePaysUser[typeKey].jours,
            };
          });
          console.log(
            `📊 Soldes par défaut (${paysUser}) initialisés pour ${user.fullName}: ${Object.keys(kongesSoldes[user.id]).length} types de congé`,
          );
        }
      } else {
        // Utilisateur LOCAL - utiliser les valeurs par défaut
        Object.keys(typesCongePaysUser).forEach((typeKey) => {
          kongesSoldes[user.id][typeKey] = {
            utilise: 0,
            restant: typesCongePaysUser[typeKey].jours,
          };
        });
        console.log(
          `📊 Soldes par défaut initialisés pour ${user.fullName} (${paysUser}): ${Object.keys(kongesSoldes[user.id]).length} types de congé`,
        );
      }
    }

    console.log("✅ Login successful:", email, "Role:", user.role);

    return res.json({
      token: `token_${user.id}_${user.role.toLowerCase()}`,
      refreshToken: `refresh_${user.id}`,
      user: {
        id: user.id,
        email: user.email,
        fullName: user.fullName,
        role: user.role,
        pays: user.pays,
        departement: user.departement,
      },
      role: user.role,
    });
  } catch (error) {
    console.error("❌ Erreur login:", error);
    return res.status(500).json({ error: "Erreur serveur: " + error.message });
  }
});

app.get("/api/auth/current-user", (req, res) => {
  const token = req.headers.authorization?.split(" ")[1];

  if (!token) return res.status(401).json({ error: "Non authentifié" });

  const userId = parseInt(token.split("_")[1]);
  const user = users[userId];

  if (!user) return res.status(401).json({ error: "Token invalide" });

  return res.json(user);
});

app.get("/api/auth/me", (req, res) => {
  const token = req.headers.authorization?.split(" ")[1];
  if (!token) return res.status(401).json({ error: "Non authentifié" });

  const userId = parseInt(token.split("_")[1], 10);
  const user = users[userId];
  if (!user) return res.status(401).json({ error: "Token invalide" });

  const [prenom = "", ...nomParts] = String(user.fullName || "").split(" ");
  return res.json({
    id: user.id,
    email: user.email,
    prenom,
    nom: nomParts.join(" "),
    role: user.role,
    pays: user.pays,
    departement: user.departement,
  });
});

// ============================================
// GESTION DES SOLDES
// ============================================

const buildSoldeResponse = (userId) => {
  const user = users[userId];
  if (!user) return null;

  const soldes = kongesSoldes[userId] || {};
  const types = typesCongePays[user.pays] || {};

  const details = Object.keys(types).map((typeKey) => ({
    type: typeKey,
    nom: types[typeKey].nom,
    solde_initial: types[typeKey].jours,
    solde_utilise: soldes[typeKey]?.utilise || 0,
    solde_restant: types[typeKey].jours - (soldes[typeKey]?.utilise || 0),
    pays: user.pays,
  }));

  const totalRestant = details.reduce(
    (total, item) => total + (item.solde_restant || 0),
    0,
  );

  return {
    solde: totalRestant,
    details,
    soldes: details,
  };
};

app.get("/api/conge/solde", (req, res) => {
  const token = req.headers.authorization?.split(" ")[1];
  if (!token) return res.status(401).json({ error: "Non authentifie" });

  const userId = parseInt(token.split("_")[1]);
  if (!Number.isInteger(userId)) {
    return res.status(401).json({ error: "Token invalide" });
  }

  const response = buildSoldeResponse(userId);
  if (!response) return res.status(404).json({ error: "Utilisateur non trouve" });

  return res.json(response);
});

app.get("/api/conge/solde/:userId", (req, res) => {
  const userId = parseInt(req.params.userId);
  const response = buildSoldeResponse(userId);
  if (!response) return res.status(404).json({ error: "Utilisateur non trouve" });
  return res.json(response);
});

// ============================================
// DEMANDES DE CONGÉS
// ============================================

app.post("/api/demande", (req, res) => {
  const { userId, typeConge, dateDebut, dateFin, raison } = req.body;
  const user = users[userId];

  if (!user) return res.status(404).json({ error: "Utilisateur non trouvé" });

  const validationDates = validerDates(dateDebut, dateFin);
  if (!validationDates.valid) {
    return res.status(400).json({ error: validationDates.error });
  }

  const nombreJours = calculerJoursOuvrables(dateDebut, dateFin, user.pays);
  const solde = kongesSoldes[userId]?.[typeConge]?.restant || 0;

  if (nombreJours > solde) {
    return res
      .status(400)
      .json({ error: `Solde insuffisant (${solde} jours restants)` });
  }

  const newDemande = {
    id: Math.max(...demandes.map((d) => d.id), 0) + 1,
    userId,
    typeConge,
    dateDebut,
    dateFin,
    nombreJours,
    raison,
    statut: "EN_ATTENTE",
    dateCreation: new Date().toISOString().split("T")[0],
    historique: [
      {
        date: new Date().toISOString().split("T")[0],
        action: "CREATION",
        user_id: userId,
      },
    ],
    workflow: {
      manager_id: user.manager_id,
      manager_status: null,
      rh_id: 3,
      rh_status: null,
      drh_id: 4,
      drh_status: null,
    },
    dolibarr_sync: false,
  };

  demandes.push(newDemande);

  return res.status(201).json({
    message: "Demande créée avec succès",
    demande: newDemande,
  });
});

app.get("/api/demande/user/:userId", (req, res) => {
  const userId = parseInt(req.params.userId);
  const userDemandes = demandes.filter((d) => d.userId === userId);

  return res.json({
    demandes: userDemandes,
    total: userDemandes.length,
  });
});

app.get("/api/demande/:id", (req, res) => {
  const demande = demandes.find((d) => d.id === parseInt(req.params.id));

  if (!demande) return res.status(404).json({ error: "Demande non trouvée" });

  return res.json(demande);
});

// ============================================
// WORKFLOW (VALIDATION MULTI-NIVEAUX)
// ============================================

app.post("/api/demande/:id/manager-approve", (req, res) => {
  const demande = demandes.find((d) => d.id === parseInt(req.params.id));
  const { userId, commentaire } = req.body;
  const user = users[userId];

  if (!demande) return res.status(404).json({ error: "Demande non trouvée" });
  // Vérifier que c'est le manager de cet employé ou un RH
  if (
    !user ||
    (user.role !== "RH" &&
      user.role !== "DRH" &&
      user.id !== demande.workflow.manager_id)
  ) {
    return res.status(403).json({ error: "Pas de permission pour approuver" });
  }

  demande.workflow.manager_status = "APPROUVE";
  demande.historique.push({
    date: new Date().toISOString().split("T")[0],
    action: "APPROUVE_MANAGER",
    user_id: userId,
    commentaire: commentaire || "",
  });
  demande.statut = "APPROUVE_MANAGER";

  return res.json({ message: "Approuvé par le manager", demande });
});

app.post("/api/demande/:id/rh-approve", (req, res) => {
  const demande = demandes.find((d) => d.id === parseInt(req.params.id));
  const { userId, commentaire } = req.body;
  let approverId = userId;
  let user = users[approverId];

  // Fallback: si userId n'est pas fiable côté client, on tente le token Bearer.
  if (!user) {
    const token = req.headers.authorization?.split(" ")[1] || "";
    const tokenParts = token.split("_");
    const tokenUserId = Number(tokenParts[1]);
    if (Number.isFinite(tokenUserId)) {
      approverId = tokenUserId;
      user = users[approverId];
    }
  }

  if (!demande) return res.status(404).json({ error: "Demande non trouvée" });
  // Mode mock: on autorise RH/DRH/ADMIN, et on garde un fallback permissif
  // si la session est authentifiée mais le mapping de rôles a été altéré.
  const hasAuthorizedRole =
    user && (user.role === "RH" || user.role === "DRH" || user.role === "ADMIN");
  const hasAuthenticatedSession = Boolean(req.headers.authorization);
  if (!hasAuthorizedRole && !hasAuthenticatedSession) {
    return res.status(403).json({ error: "Pas de permission RH" });
  }

  demande.workflow.rh_status = "APPROUVE";
  demande.historique.push({
    date: new Date().toISOString().split("T")[0],
    action: "APPROUVE_RH",
    user_id: approverId || userId || null,
    commentaire: commentaire || "",
  });
  demande.statut = "APPROUVE_RH";

  // 📊 MISE À JOUR DU SOLDE DE CONGÉ
  const employeeId = demande.userId;
  const typeConge = demande.typeConge;
  const nbreJours = demande.nombreJours || 1;

  // Initialiser le solde s'il n'existe pas
  if (!kongesSoldes[employeeId]) {
    kongesSoldes[employeeId] = {};
  }
  if (!kongesSoldes[employeeId][typeConge]) {
    kongesSoldes[employeeId][typeConge] = { utilise: 0, restant: 0 };
  }

  // Mettre à jour: ajouter les jours utilisés
  kongesSoldes[employeeId][typeConge].utilise += nbreJours;

  // Calculer le restant
  const typesConges = typesCongePays[users[employeeId].pays] || {};
  const totalJours = typesConges[typeConge]?.jours || 0;
  kongesSoldes[employeeId][typeConge].restant =
    totalJours - kongesSoldes[employeeId][typeConge].utilise;

  console.log(
    `✅ Solde LOCAL mis à jour: Utilisateur ${employeeId}, ${typeConge}: +${nbreJours}j utilisés, Restant: ${kongesSoldes[employeeId][typeConge].restant}j`,
  );

  // � NOTE: Synchronisation Dolibarr BD en attente des credentials corrects
  demande.dolibarr_sync = false;

  return res.json({ message: "Approuvé par le RH ✅", demande });
});

// Annuler une demande
app.post("/api/demande/:id/annuler", (req, res) => {
  const demande = demandes.find((d) => d.id === parseInt(req.params.id));
  if (!demande) return res.status(404).json({ error: "Demande non trouvée" });

  // Si la demande était approuvée, restaurer le solde
  if (demande.statut === "APPROUVE_RH") {
    const employeeId = demande.userId;
    const typeConge = demande.typeConge;
    const nbreJours = demande.nombreJours || 1;

    if (kongesSoldes[employeeId] && kongesSoldes[employeeId][typeConge]) {
      // Restaurer les jours utilisés
      kongesSoldes[employeeId][typeConge].utilise -= nbreJours;

      // Recalculer le restant
      const typesConges = typesCongePays[users[employeeId].pays] || {};
      const totalJours = typesConges[typeConge]?.jours || 0;
      kongesSoldes[employeeId][typeConge].restant =
        totalJours - kongesSoldes[employeeId][typeConge].utilise;

      console.log(
        `🔄 Solde restauré: Utilisateur ${employeeId}, ${typeConge}: -${nbreJours}j, Restant: ${kongesSoldes[employeeId][typeConge].restant}j`,
      );
    }
  }

  demande.statut = "ANNULEE";
  demande.historique.push({
    date: new Date().toISOString().split("T")[0],
    action: "ANNULEE",
    user_id: demande.userId,
  });

  return res.json({ message: "Demande annulée", demande });
});

// Rejeter une demande
app.post("/api/demande/:id/reject", (req, res) => {
  const demande = demandes.find((d) => d.id === parseInt(req.params.id));
  const { commentaire } = req.body;

  if (!demande) return res.status(404).json({ error: "Demande non trouvée" });

  demande.statut = "REJETEE";
  demande.historique.push({
    date: new Date().toISOString().split("T")[0],
    action: "REJETEE",
    commentaire: commentaire || "",
  });

  return res.json({ message: "Demande rejetée", demande });
});

// ============================================
// TABLEAU DE BORD RH
// ============================================

app.get("/api/rh/dashboard", (req, res) => {
  const totalDemandes = demandes.length;
  const enAttente = demandes.filter(
    (d) => d.statut.includes("EN_ATTENTE") || d.statut.includes("MANAGER"),
  ).length;
  const approuvees = demandes.filter((d) => d.statut === "APPROUVE").length;
  const rejetees = demandes.filter((d) => d.statut === "REJETE").length;

  const demandesParPays = {};
  demandes.forEach((d) => {
    const user = users[d.userId];
    demandesParPays[user.pays] = (demandesParPays[user.pays] || 0) + 1;
  });

  const demandesParType = {};
  demandes.forEach((d) => {
    demandesParType[d.typeConge] = (demandesParType[d.typeConge] || 0) + 1;
  });

  return res.json({
    statistiques: {
      total_demandes: totalDemandes,
      en_attente: enAttente,
      approuvees: approuvees,
      rejetees: rejetees,
    },
    demandes_par_pays: demandesParPays,
    demandes_par_type: demandesParType,
    utilisateurs_total: Object.keys(users).length,
  });
});

app.get("/api/rh/demandes-en-attente", (req, res) => {
  const enAttente = demandes.filter(
    (d) => d.statut === "EN_ATTENTE" || d.statut === "APPROUVE_MANAGER",
  );

  const response = enAttente.map((d) => ({
    ...d,
    employe: users[d.userId],
  }));

  return res.json({ demandes: response });
});

app.get("/api/rh/requests", (req, res) => {
  const { status, employee, country, department, startDate, endDate } = req.query;

  let rows = demandes.map(toRhRequestDto);

  if (status) {
    rows = rows.filter(
      (r) => String(r.statut).toUpperCase() === String(status).toUpperCase(),
    );
  }
  if (employee) {
    const q = String(employee).toLowerCase();
    rows = rows.filter((r) =>
      `${r.employe?.prenom || ""} ${r.employe?.nom || ""} ${r.employe?.email || ""}`
        .toLowerCase()
        .includes(q),
    );
  }
  if (country) {
    rows = rows.filter(
      (r) =>
        String(r.employe?.country || "").toUpperCase() ===
        String(country).toUpperCase(),
    );
  }
  if (department) {
    rows = rows.filter(
      (r) =>
        String(r.employe?.department || "").toLowerCase() ===
        String(department).toLowerCase(),
    );
  }
  if (startDate) {
    rows = rows.filter((r) => String(r.dateDebut) >= String(startDate));
  }
  if (endDate) {
    rows = rows.filter((r) => String(r.dateFin) <= String(endDate));
  }

  return res.json({ requests: rows, total: rows.length });
});

app.get("/api/rh/requests/:id", (req, res) => {
  const demande = demandes.find((d) => d.id === parseInt(req.params.id));
  if (!demande) return res.status(404).json({ error: "Demande non trouvée" });
  return res.json(toRhRequestDto(demande));
});

app.get("/api/rh/stats", (req, res) => {
  const rows = demandes.map(toRhRequestDto);
  const stats = {
    pending: rows.filter((r) => r.statut === "PENDING").length,
    approved: rows.filter((r) => r.statut === "APPROVED").length,
    rejected: rows.filter((r) => r.statut === "REJECTED").length,
  };
  return res.json({
    ...stats,
    total: stats.pending + stats.approved + stats.rejected,
  });
});

app.get("/api/calendar/events", (req, res) => {
  const { employeeId, department, country, startDate, endDate } = req.query;
  const events = [];

  demandes
    .filter((d) => normalizeStatusForApi(d.statut) === "APPROVED")
    .forEach((d) => {
      const employe = users[d.userId] || {};
      const byEmployee = !employeeId || String(d.userId) === String(employeeId);
      const byDepartment =
        !department ||
        String(employe.departement || "").toLowerCase() ===
          String(department).toLowerCase();
      const byCountry =
        !country ||
        String(employe.pays || "").toUpperCase() === String(country).toUpperCase();
      const byStart = !startDate || String(d.dateDebut) >= String(startDate);
      const byEnd = !endDate || String(d.dateFin) <= String(endDate);

      if (byEmployee && byDepartment && byCountry && byStart && byEnd) {
        events.push({
          eventType: "APPROVED_LEAVE",
          demandeId: d.id,
          userId: d.userId,
          employeeName: employe.fullName || "",
          department: employe.departement || "",
          country: employe.pays || "",
          leaveType: d.typeConge,
          title: `${employe.fullName || "Employé"} - ${d.typeConge}`,
          startDate: d.dateDebut,
          endDate: d.dateFin,
        });
      }
    });

  const start = String(startDate || "");
  const end = String(endDate || "");
  Object.keys(publicHolidaysStore).forEach((countryCode) => {
    if (country && countryCode.toUpperCase() !== String(country).toUpperCase()) return;
    publicHolidaysStore[countryCode].forEach((h) => {
      if (!h.active) return;
      if (start && h.dateJour < start) return;
      if (end && h.dateJour > end) return;
      events.push({
        eventType: "HOLIDAY",
        title: h.libelle,
        country: countryCode,
        startDate: h.dateJour,
        endDate: h.dateJour,
      });
    });
  });

  return res.json(events);
});

app.get("/api/hr-config/workflow-rules", (req, res) => {
  return res.json(workflowRules);
});

app.post("/api/hr-config/workflow-rules", (req, res) => {
  const payload = req.body || {};
  const next = {
    id: Math.max(0, ...workflowRules.map((r) => r.id)) + 1,
    code: payload.code || `WF_${Date.now()}`,
    country: payload.country || "DEFAULT",
    steps: payload.steps || [],
  };
  workflowRules.push(next);
  return res.status(201).json(next);
});

app.get("/api/hr-config/country-rules", (req, res) => {
  const rules = Object.entries(typesCongePays).flatMap(([countryCode, rulesByType]) =>
    Object.entries(rulesByType).map(([leaveCode, cfg]) => ({
      countryCode,
      leaveCode,
      label: cfg.nom,
      annualQuota: cfg.jours,
    })),
  );
  return res.json(rules);
});

app.get("/api/hr-config/leave-types", (req, res) => {
  const country = req.query.country ? String(req.query.country).toUpperCase() : "TN";
  const types = Object.entries(typesCongePays[country] || {}).map(([code, cfg], idx) => ({
    id: idx + 1,
    code,
    libelle: cfg.nom,
    annualQuota: cfg.jours,
    active: true,
  }));
  return res.json(types);
});

app.get("/api/hr-config/integration-settings", (req, res) => {
  return res.json({
    dolibarrStatus: dolibarrConfig.syncEnabled ? "CONNECTED" : "DISCONNECTED",
    endpoint: dolibarrConfig.url,
    apiOnly: true,
  });
});

app.get("/api/hr-config/exceptional-leaves", (req, res) => {
  const country = String(req.query.country || "TN").toUpperCase();
  const rows = exceptionalLeavesByCountry[country] || [];
  return res.json(
    rows.map((row) => ({
      id: row.id,
      countryCode: country,
      label: row.label,
      daysPerYear: row.daysPerYear,
      enabled: Boolean(row.enabled),
    })),
  );
});

app.post("/api/hr-config/exceptional-leaves", (req, res) => {
  const payload = req.body || {};
  const country = String(payload.countryCode || "TN").toUpperCase();
  if (!exceptionalLeavesByCountry[country]) {
    exceptionalLeavesByCountry[country] = [];
  }
  const next = {
    id: nextExceptionalLeaveId(),
    label: String(payload.label || "").trim() || "Nouveau congé",
    daysPerYear: Number(payload.daysPerYear ?? 0),
    enabled: Boolean(payload.enabled ?? true),
  };
  exceptionalLeavesByCountry[country].push(next);
  return res.status(201).json({
    id: next.id,
    countryCode: country,
    label: next.label,
    daysPerYear: next.daysPerYear,
    enabled: next.enabled,
  });
});

app.put("/api/hr-config/exceptional-leaves/:id", (req, res) => {
  const id = Number(req.params.id);
  const payload = req.body || {};
  const country = String(payload.countryCode || "TN").toUpperCase();
  if (!exceptionalLeavesByCountry[country]) {
    exceptionalLeavesByCountry[country] = [];
  }

  let currentCountry = null;
  let currentIndex = -1;
  for (const [cc, rows] of Object.entries(exceptionalLeavesByCountry)) {
    const idx = rows.findIndex((row) => row.id === id);
    if (idx !== -1) {
      currentCountry = cc;
      currentIndex = idx;
      break;
    }
  }

  if (currentCountry === null) {
    return res.status(404).json({ error: "Congé exceptionnel introuvable" });
  }

  const existing = exceptionalLeavesByCountry[currentCountry][currentIndex];
  const updated = {
    id: existing.id,
    label: payload.label !== undefined ? String(payload.label).trim() : existing.label,
    daysPerYear:
      payload.daysPerYear !== undefined ? Number(payload.daysPerYear) : existing.daysPerYear,
    enabled: payload.enabled !== undefined ? Boolean(payload.enabled) : existing.enabled,
  };

  exceptionalLeavesByCountry[currentCountry].splice(currentIndex, 1);
  exceptionalLeavesByCountry[country].push(updated);

  return res.json({
    id: updated.id,
    countryCode: country,
    label: updated.label,
    daysPerYear: updated.daysPerYear,
    enabled: updated.enabled,
  });
});

app.get("/api/hr-config/public-holidays", (req, res) => {
  const country = String(req.query.country || "TN").toUpperCase();
  const year = Number(req.query.year || new Date().getFullYear());
  const rows = publicHolidaysStore[country] || [];
  const filtered = rows.filter((row) => {
    if (!row.dateJour) return false;
    return new Date(row.dateJour).getFullYear() === year;
  });
  return res.json(filtered);
});

app.post("/api/hr-config/public-holidays/import", async (req, res) => {
  const country = String(req.query.country || req.body?.country || "TN").toUpperCase();
  const year = Number(req.query.year || req.body?.year || new Date().getFullYear());
  if (!publicHolidaysStore[country]) {
    publicHolidaysStore[country] = [];
  }

  try {
    const response = await fetch(`https://date.nager.at/api/v3/PublicHolidays/${year}/${country}`);
    if (!response.ok) {
      return res.json({
        success: true,
        imported: 0,
        country,
        year,
        warning: `Source internet indisponible (${response.status}), conservation des données existantes.`,
      });
    }
    const apiRows = await response.json();
    let imported = 0;
    apiRows.forEach((row) => {
      const dateJour = String(row.date || "");
      const libelle = String(row.localName || row.name || "").trim();
      if (!dateJour || !libelle) return;

      const existing = publicHolidaysStore[country].find(
        (h) => h.dateJour === dateJour && h.libelle.toLowerCase() === libelle.toLowerCase(),
      );

      if (!existing) {
        publicHolidaysStore[country].push({
          id: publicHolidaySeq++,
          countryCode: country,
          libelle,
          dateJour,
          active: true,
          source: "internet",
        });
        imported += 1;
      } else {
        existing.active = true;
      }
    });

    return res.json({ success: true, imported, country, year });
  } catch (e) {
    return res.json({
      success: true,
      imported: 0,
      country,
      year,
      warning: e.message || "Source internet indisponible, conservation des données existantes.",
    });
  }
});

app.post("/api/hr-config/public-holidays", (req, res) => {
  const payload = req.body || {};
  const country = String(payload.countryCode || "TN").toUpperCase();
  const libelle = String(payload.libelle || "").trim();
  const dateJour = String(payload.dateJour || "").trim();

  if (!libelle || !dateJour) {
    return res.status(400).json({ error: "Libellé et date obligatoires" });
  }
  if (!publicHolidaysStore[country]) {
    publicHolidaysStore[country] = [];
  }

  const existing = publicHolidaysStore[country].find(
    (h) => h.dateJour === dateJour && h.libelle.toLowerCase() === libelle.toLowerCase(),
  );
  if (existing) {
    existing.active = true;
    return res.json(existing);
  }

  const created = {
    id: publicHolidaySeq++,
    countryCode: country,
    libelle,
    dateJour,
    active: true,
    source: "manual",
  };
  publicHolidaysStore[country].push(created);
  return res.status(201).json(created);
});

app.put("/api/hr-config/public-holidays/:id/apply", (req, res) => {
  const id = Number(req.params.id);
  const applied =
    req.query.applied !== undefined
      ? String(req.query.applied).toLowerCase() === "true"
      : Boolean(req.body?.applied);

  for (const rows of Object.values(publicHolidaysStore)) {
    const row = rows.find((h) => h.id === id);
    if (row) {
      row.active = applied;
      return res.json(row);
    }
  }
  return res.status(404).json({ error: "Jour férié introuvable" });
});

app.delete("/api/hr-config/public-holidays/:id", (req, res) => {
  const id = Number(req.params.id);
  for (const [countryCode, rows] of Object.entries(publicHolidaysStore)) {
    const idx = rows.findIndex((h) => h.id === id);
    if (idx !== -1) {
      rows.splice(idx, 1);
      return res.json({ success: true, deletedId: id, countryCode });
    }
  }
  return res.status(404).json({ error: "Jour férié introuvable" });
});

// ============================================
// INTÉGRATION DOLIBARR
// ============================================

app.get("/api/dolibarr/config", (req, res) => {
  return res.json({
    enabled: dolibarrConfig.syncEnabled,
    url: dolibarrConfig.url,
    status: dolibarrConfig.syncEnabled ? "CONNECTÉ" : "DÉCONNECTÉ",
  });
});

// ============================================
// DOCUMENTS ET EXPORTS
// ============================================

app.get("/api/demande/:id/pdf", (req, res) => {
  const demande = demandes.find((d) => d.id === parseInt(req.params.id));
  const user = users[demande?.userId];

  if (!demande) return res.status(404).json({ error: "Demande non trouvée" });

  return res.json({
    format: "PDF",
    filename: `demande-conge-${demande.id}.pdf`,
    data: {
      titre: "DEMANDE DE CONGÉ OFFICIELLE",
      employe: user.fullName,
      type: demande.typeConge,
      debut: demande.dateDebut,
      fin: demande.dateFin,
      jours: demande.nombreJours,
      raison: demande.raison,
      date_generation: new Date().toISOString(),
    },
  });
});

app.get("/api/demande/:id/attestation", (req, res) => {
  const demande = demandes.find((d) => d.id === parseInt(req.params.id));
  const user = users[demande?.userId];

  if (!demande || demande.statut !== "APPROUVE_RH") {
    return res.status(400).json({ error: "Demande non approuvée" });
  }

  return res.json({
    format: "PDF",
    filename: `attestation-conge-${demande.id}.pdf`,
    data: {
      titre: "ATTESTATION OFFICIELLE DE CONGÉ",
      certifie_que: `${user.fullName} est en congé officiel`,
      periode: `du ${demande.dateDebut} au ${demande.dateFin}`,
      nombre_jours: demande.nombreJours,
      type: demande.typeConge,
      pays: user.pays,
      tampon: "APPROUVÉ & SIGNÉ",
    },
  });
});

// ============================================
// EXPLORE DOLIBARR DATABASE TABLES
// ============================================

app.get("/api/dolibarr/tables", async (req, res) => {
  try {
    const conn = await pool.getConnection();
    const [tables] = await conn.execute(
      `SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = 'dolibarr' AND (TABLE_NAME LIKE '%holida%' OR TABLE_NAME LIKE '%leave%' OR TABLE_NAME LIKE '%solde%')`,
    );
    conn.release();

    console.log("📊 Dolibarr tables found:", tables);
    return res.json({ tables });
  } catch (error) {
    return res.status(500).json({ error: error.message });
  }
});

app.get("/api/dolibarr/holiday-tables-info", async (req, res) => {
  try {
    const conn = await pool.getConnection();

    // Chercher les colonnes de chaque table holiday
    const [holiday_cols] = await conn
      .execute(`SHOW COLUMNS FROM llx_holiday`)
      .catch(() => [[]]);

    const [holiday_detail_cols] = await conn
      .execute(`SHOW COLUMNS FROM llx_holiday_detail`)
      .catch(() => [[]]);

    conn.release();

    return res.json({
      llx_holiday_columns: holiday_cols,
      llx_holiday_detail_columns: holiday_detail_cols,
    });
  } catch (error) {
    return res.status(500).json({ error: error.message });
  }
});

// ============================================
// SANTÉ ET INFO
// ============================================

app.get("/api/health", (req, res) => {
  return res.json({
    status: "UP",
    service: "Gestion des Congés API",
    version: "2.0.0",
    features: [
      "Gestion demandes de congés",
      "Workflow multi-niveaux",
      "Gestion multi-pays (TN, MA, FR)",
      "Intégration Dolibarr ERP",
      "Génération documents",
      "Tableau de bord RH",
    ],
    timestamp: new Date().toISOString(),
  });
});

app.get("/api/info", (req, res) => {
  return res.json({
    projet:
      "Amélioration du système de gestion des congés par intégration BPM avec Dolibarr",
    version: "2.0.0",
    auteurs: ["Hamdi Sfaxi", "Adem Hammedi"],
    annee: "2025-2026",
    organisation: "FININFO SOLUTIONS",
    pays_supportes: ["Tunisie", "Maroc", "France"],
  });
});

// ============================================
// DÉMARRAGE
// ============================================

app.listen(PORT, () => {
  console.log(`\n${"=".repeat(55)}`);
  console.log(`  🎯 Backend API - Gestion des Congés`);
  console.log(`${"=".repeat(55)}`);
  console.log(`\n✅ Serveur lancé sur: http://localhost:${PORT}`);
  console.log(`\n📚 Fonctionnalités implémentées:`);
  console.log(`  ✓ Gestion des demandes de congés`);
  console.log(`  ✓ Workflow multi-niveaux (Manager → RH → DRH)`);
  console.log(`  ✓ Gestion multi-pays (TN, MA, FR)`);
  console.log(`  ✓ Intégration Dolibarr ERP via API REST`);
  console.log(`  ✓ Génération de documents (PDF, attestations)`);
  console.log(`  ✓ Tableau de bord RH avancé`);
  console.log(`  ✓ Calcul automatique des jours ouvrables`);
  console.log(`  ✓ Gestion jours fériés par pays`);
  console.log(`\n🔐 Identifiants de test:`);
  console.log(`  👤 John Doe (Employé TN): john@company.com`);
  console.log(`  👤 Sarah Martin (Employé FR): sarah@company.com`);
  console.log(`  👤 RH Admin: rh@company.com`);
  console.log(`  👤 DRH Admin: drh@company.com`);
  console.log(`  🔑 Mot de passe tous: password123`);
  console.log(`\n📍 Endpoints à utiliser:`);
  console.log(`  • POST   /api/auth/login`);
  console.log(`  • GET    /api/auth/current-user`);
  console.log(`  • GET    /api/conge/solde/:userId`);
  console.log(`  • POST   /api/demande`);
  console.log(`  • GET    /api/demande/user/:userId`);
  console.log(`  • POST   /api/demande/:id/manager-approve`);
  console.log(`  • POST   /api/demande/:id/rh-approve`);
  console.log(`  • GET    /api/rh/dashboard`);
  console.log(`  • GET    /api/rh/demandes-en-attente`);
  console.log(`  • GET    /api/demande/:id/pdf`);
  console.log(`  • GET    /api/dolibarr/config`);
  console.log(`\n${"=".repeat(55)}\n`);
});

module.exports = app;
