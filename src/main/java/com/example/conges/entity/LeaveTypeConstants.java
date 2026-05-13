package com.example.conges.entity;

/**
 * Constantes pour les codes de types de congés Dolibarr
 * Ces codes sont stables et portables entre différentes instances Dolibarr
 */
public final class LeaveTypeConstants {

    // Codes métier standards pour les congés
    public static final String LEAVE_RTT_FR = "LEAVE_RTT_FR";
    public static final String LEAVE_PAID_FR = "LEAVE_PAID_FR";

    // Codes pour autres types de congés (à étendre selon les besoins)
    public static final String LEAVE_SICK = "LEAVE_SICK";
    public static final String LEAVE_UNPAID = "LEAVE_UNPAID";
    public static final String LEAVE_MATERNITY = "LEAVE_MATERNITY";

    private LeaveTypeConstants() {
        // Classe utilitaire, pas d'instanciation
    }

    /**
     * Vérifie si le code correspond à un congé payé
     */
    public static boolean isPaidLeave(String code) {
        return LEAVE_PAID_FR.equals(code) || LEAVE_RTT_FR.equals(code);
    }

    /**
     * Vérifie si le code correspond à un congé RTT
     */
    public static boolean isRttLeave(String code) {
        return LEAVE_RTT_FR.equals(code);
    }
}