-- Exécuter manuellement sur MySQL/MariaDB si besoin (ignorer erreur duplicate column).
-- Prérequis hors prod : faire une sauvegarde avant ALTER.

ALTER TABLE country_leave_policies
    ADD COLUMN monthly_accrual_rate DOUBLE NULL COMMENT 'Jours CP acquis par mois (prorata année)' AFTER annual_quota;

ALTER TABLE country_leave_policies
    ADD COLUMN rtt_enabled TINYINT(1) NOT NULL DEFAULT 0 COMMENT 'RTT autorises pays' AFTER monthly_accrual_rate;

ALTER TABLE country_leave_policies
    ADD COLUMN rtt_annual_days INT NULL COMMENT 'Quota RTT annuel FR' AFTER rtt_enabled;

ALTER TABLE users
    ADD COLUMN hire_date DATE NULL COMMENT 'Date embauche prorata' AFTER pays;
