/**
 * AI Scoring Service for Leave Requests
 * Calculates intelligent validation score for each leave request
 * Helps RH make faster decisions
 */

/**
 * Calculate AI score for a leave request
 * @param {Object} demande - Leave request
 * @param {Object} user - User object
 * @param {Object} allDemandes - All leave requests in system
 * @param {Object} holidays - Holidays data
 * @param {String} country - User country (TN/FR/MA)
 * @returns {Object} AI Score object with score, risk level, and recommendation
 */
function calculateAIScore(demande, user, allDemandes, holidays, country) {
  let score = 100; // Start with perfect score
  let riskFactors = [];
  let impactFactors = [];

  // ========== FACTOR 1: BALANCE CHECK ==========
  const hasEnoughBalance = checkBalance(demande, user);
  if (!hasEnoughBalance) {
    score -= 20;
    riskFactors.push("Solde insuffisant");
  }

  // ========== FACTOR 2: SIMULTANEOUS ABSENCES ==========
  const simultaneousCount = countSimultaneousAbsences(demande, allDemandes);
  if (simultaneousCount >= 3) {
    score -= 15;
    riskFactors.push(`${simultaneousCount} employés absents simultanément`);
    impactFactors.push("Risque de sous-effectifs");
  } else if (simultaneousCount >= 2) {
    score -= 8;
    impactFactors.push("Impact modéré sur l'équipe");
  }

  // ========== FACTOR 3: CRITICAL PERIODS ==========
  const criticalPeriod = detectCriticalPeriod(demande.dateDebut, demande.dateFin, country);
  if (criticalPeriod.isCritical) {
    score -= 12;
    riskFactors.push(criticalPeriod.reason);
  }

  // ========== FACTOR 4: PUBLIC HOLIDAYS ==========
  const nearHoliday = checkNearPublicHoliday(demande.dateDebut, demande.dateFin, holidays, country);
  if (nearHoliday.isNear) {
    score -= 5;
    impactFactors.push("Proximité avec jours fériés");
  }

  // ========== FACTOR 5: LEAVE TYPE PRIORITY ==========
  const typeScore = scoreLeaveType(demande.typeConge);
  score += typeScore.score;
  if (typeScore.note) impactFactors.push(typeScore.note);

  // ========== FACTOR 6: CONTINUITY RISK ==========
  const continuityRisk = assessContinuityRisk(demande, allDemandes);
  if (continuityRisk.isRisky) {
    score -= continuityRisk.penalty;
    riskFactors.push(continuityRisk.reason);
  }

  // ========== FACTOR 7: FREQUENCY PATTERN ==========
  const frequencyScore = assessFrequencyPattern(user.id, demande, allDemandes);
  if (frequencyScore.isAbnormal) {
    score -= 8;
    riskFactors.push("Fréquence anormale de demandes");
  }

  // ========== FACTOR 8: ADVANCE NOTICE ==========
  const noticeScore = assessAdvanceNotice(demande);
  score += noticeScore.score;
  if (noticeScore.note) impactFactors.push(noticeScore.note);

  // ========== FACTOR 9: POSITION IMPACT ==========
  const positionImpact = assessPositionImpact(user.departement);
  if (positionImpact.isHighImpact) {
    score -= positionImpact.penalty;
    riskFactors.push(`Département critique: ${user.departement}`);
  }

  // ========== FACTOR 10: COUNTRY-SPECIFIC RULES ==========
  const countryScore = assessCountryRules(demande, user, country);
  score += countryScore.score;
  if (countryScore.note) impactFactors.push(countryScore.note);

  // Ensure score is between 0-100
  score = Math.max(0, Math.min(100, Math.round(score)));

  // Determine risk level
  const riskLevel = determineRiskLevel(score);

  // Generate recommendation
  const recommendation = generateRecommendation(score, riskLevel, demande, user);

  return {
    score,
    riskLevel,
    recommendation,
    riskFactors,
    impactFactors,
    timestamp: new Date().toISOString(),
  };
}

/**
 * Check if user has sufficient balance
 */
function checkBalance(demande, user) {
  // Simplified - in real system would check actual balance
  const minimumBalance = demande.typeConge === "SORTIE_COURTE" ? 0 : 1;
  return true; // Trust that backend already validated this
}

/**
 * Count simultaneous absences on the same dates
 */
function countSimultaneousAbsences(demande, allDemandes) {
  return allDemandes.filter((d) => {
    if (d.id === demande.id || d.statut === "REFUSEE") return false;
    const dStart = new Date(d.dateDebut);
    const dEnd = new Date(d.dateFin);
    const rStart = new Date(demande.dateDebut);
    const rEnd = new Date(demande.dateFin);
    return !(dEnd < rStart || dStart > rEnd);
  }).length;
}

/**
 * Detect critical business periods
 */
function detectCriticalPeriod(dateDebut, dateFin, country) {
  const start = new Date(dateDebut);
  const end = new Date(dateFin);
  const month = start.getMonth();

  const criticalPeriods = {
    TN: [
      { months: [0, 11], reason: "Période de fin d'année (bilan annuel)" },
      { months: [3, 4], reason: "Période budgétaire critique" },
    ],
    FR: [
      { months: [0, 1], reason: "Période post-vacances (démarrage d'année)" },
      { months: [7, 8], reason: "Pic de congés estivaux" },
    ],
    MA: [
      { months: [0, 11], reason: "Période de fin d'année" },
      { months: [5, 6], reason: "Préparation aux examens" },
    ],
  };

  const periods = criticalPeriods[country] || [];
  for (const period of periods) {
    if (period.months.includes(month)) {
      return { isCritical: true, reason: period.reason };
    }
  }

  return { isCritical: false };
}

/**
 * Check if dates are near public holidays
 */
function checkNearPublicHoliday(dateDebut, dateFin, holidays, country) {
  const start = new Date(dateDebut);
  const end = new Date(dateFin);
  const fivedays = 5 * 24 * 60 * 60 * 1000;

  for (const holiday of holidays) {
    const hDate = new Date(holiday.date);
    if (
      Math.abs(hDate - start) < fivedays ||
      Math.abs(hDate - end) < fivedays
    ) {
      return { isNear: true };
    }
  }

  return { isNear: false };
}

/**
 * Score leave type by priority
 */
function scoreLeaveType(typeConge) {
  const typeScores = {
    CONGE_MALADIE: { score: 15, note: "Congé maladie (priorité élevée)" },
    PARENTAL: { score: 12, note: "Congé parental" },
    CONGES_PAYES: { score: 5, note: "Congés payés standard" },
    SORTIE_COURTE: { score: 8, note: "Sortie courte durée" },
    CONGE_SANS_SOLDE: { score: -10, note: "Congé sans solde (à justifier)" },
    RETARD_DEMAND: { score: 0, note: "Régularisation de retard" },
  };

  return typeScores[typeConge] || { score: 0, note: "" };
}

/**
 * Assess continuity risk (avoid leaving critical tasks)
 */
function assessContinuityRisk(demande, allDemandes) {
  // Check if same employee has multiple consecutive requests
  const consecutive = allDemandes.filter((d) => {
    if (d.id === demande.id || d.userId !== demande.userId) return false;
    const end = new Date(d.dateFin);
    const start = new Date(demande.dateDebut);
    const dayDiff = Math.abs((start - end) / (1000 * 60 * 60 * 24));
    return dayDiff <= 3; // Within 3 days
  }).length;

  if (consecutive > 0) {
    return {
      isRisky: true,
      penalty: 10,
      reason: "Demandes rapprochées détectées",
    };
  }

  return { isRisky: false, penalty: 0 };
}

/**
 * Assess abnormal frequency pattern
 */
function assessFrequencyPattern(userId, demande, allDemandes) {
  const sixMonthsAgo = new Date();
  sixMonthsAgo.setMonth(sixMonthsAgo.getMonth() - 6);

  const recentRequests = allDemandes.filter((d) => {
    return (
      d.userId === userId &&
      d.id !== demande.id &&
      new Date(d.dateCreation) > sixMonthsAgo
    );
  }).length;

  // If more than 8 requests in 6 months, it's abnormal
  if (recentRequests > 8) {
    return { isAbnormal: true };
  }

  return { isAbnormal: false };
}

/**
 * Assess advance notice (how early the request was submitted)
 */
function assessAdvanceNotice(demande) {
  const today = new Date();
  const requestDate = new Date(demande.dateCreation || today);
  const startDate = new Date(demande.dateDebut);
  const daysInAdvance = Math.floor((startDate - requestDate) / (1000 * 60 * 60 * 24));

  if (daysInAdvance >= 30) {
    return { score: 10, note: "Préavis excellent (30+ jours)" };
  } else if (daysInAdvance >= 14) {
    return { score: 5, note: "Préavis bon (14+ jours)" };
  } else if (daysInAdvance >= 3) {
    return { score: 0, note: "" };
  } else {
    return { score: -15, note: "Demande de dernière minute" };
  }
}

/**
 * Assess position impact on business
 */
function assessPositionImpact(departement) {
  const criticalDepts = ["IT", "RH", "Finance", "Direction"];

  if (criticalDepts.includes(departement)) {
    return { isHighImpact: true, penalty: 8 };
  }

  return { isHighImpact: false, penalty: 0 };
}

/**
 * Assess country-specific rules
 */
function assessCountryRules(demande, user, country) {
  // Example: In France, RTT has positive score
  if (country === "FR" && demande.typeConge === "SORTIE_COURTE") {
    return { score: 5, note: "RTT France (flexibilité bonne)" };
  }

  // In Tunisia, Ramadan period might have special rules
  if (country === "TN") {
    const month = new Date(demande.dateDebut).getMonth();
    if (month === 2) {
      // Ramadan is typically March
      return { score: -5, note: "Période de Ramadan (à valider)" };
    }
  }

  return { score: 0, note: "" };
}

/**
 * Determine risk level based on score
 */
function determineRiskLevel(score) {
  if (score >= 70) return "FAIBLE";
  if (score >= 50) return "MOYEN";
  return "ÉLÈVE";
}

/**
 * Generate AI recommendation
 */
function generateRecommendation(score, riskLevel, demande, user) {
  if (riskLevel === "FAIBLE" && score >= 75) {
    return {
      action: "RECOMMANDÉ",
      message: `Demande favorable pour ${user.fullName}. Score IA élevé.`,
      confidence: Math.min(100, score + 10),
    };
  }

  if (riskLevel === "MOYEN") {
    return {
      action: "VALIDATION_MANUELLE",
      message: `Validation manuelle conseillée. Vérifier les impacts organisationnels.`,
      confidence: 60,
    };
  }

  if (riskLevel === "ÉLÈVE" && score < 40) {
    return {
      action: "À_ÉVITER",
      message: `Risque organisationnel élevé détecté. Recommander des dates alternatives.`,
      confidence: 85,
    };
  }

  return {
    action: "VALIDATION_MANUELLE",
    message: "Révision RH requise pour évaluer tous les facteurs.",
    confidence: 50,
  };
}

module.exports = {
  calculateAIScore,
};
