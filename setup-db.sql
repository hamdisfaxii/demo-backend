-- Script de création de la base de données et des tables pour l'application de gestion des congés

-- Créer la base de données si elle n'existe pas
CREATE DATABASE IF NOT EXISTS conges_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Dolibarr DB (source de vérité) pour les soldes officiels
-- (en local/dev, on crée un schéma minimal compatible)
CREATE DATABASE IF NOT EXISTS dolibarr CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE conges_db;

-- Types d'énumération pour les statuts
CREATE TABLE IF NOT EXISTS typconge (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(32) NOT NULL UNIQUE,
    nom VARCHAR(255) NOT NULL,
    description TEXT,
    jours_autorises INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS statut_conge (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(32) NOT NULL UNIQUE,
    nom VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Table des rôles
CREATE TABLE IF NOT EXISTS role (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nom VARCHAR(50) NOT NULL UNIQUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Table des utilisateurs
CREATE TABLE IF NOT EXISTS user_entity (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(255) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    full_name VARCHAR(255),
    role_id BIGINT NOT NULL,
    active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (role_id) REFERENCES role(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Table des types de congés
CREATE TABLE IF NOT EXISTS leave_type (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    nom VARCHAR(255) NOT NULL,
    description TEXT,
    jours_autorises INT DEFAULT 30,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Table des jours fériés
CREATE TABLE IF NOT EXISTS holiday (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(50) NOT NULL,
    nom VARCHAR(255) NOT NULL,
    date_jour DATE NOT NULL,
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_holiday_date (date_jour),
    INDEX idx_date (date_jour)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Table d'allocation de congés par employé
CREATE TABLE IF NOT EXISTS employee_leave_allocation (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    leave_type_id BIGINT NOT NULL,
    annee_fiscale INT NOT NULL,
    solde_initial INT DEFAULT 30,
    solde_consomme INT DEFAULT 0,
    solde_restant INT DEFAULT 30,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES user_entity(id),
    FOREIGN KEY (leave_type_id) REFERENCES leave_type(id),
    UNIQUE KEY uk_allocation (user_id, leave_type_id, annee_fiscale)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Table des demandes de congé
CREATE TABLE IF NOT EXISTS demandes_conge (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    type_conge VARCHAR(32) NOT NULL,
    date_debut DATE NOT NULL,
    date_fin DATE NOT NULL,
    nombre_jours INT NOT NULL,
    raison TEXT,
    statut_demande VARCHAR(50) DEFAULT 'EN_ATTENTE',
    commentaire_rh TEXT,
    date_creation TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    date_mise_a_jour TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    date_validation TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES user_entity(id),
    INDEX idx_user_date (user_id, date_debut),
    INDEX idx_statut (statut_demande)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Insérer les rôles par défaut
INSERT IGNORE INTO role (id, nom) VALUES
(1, 'EMPLOYEE'),
(2, 'RH'),
(3, 'ADMIN');

-- Insérer les types de congés
INSERT IGNORE INTO leave_type (code, nom, description, jours_autorises) VALUES
('CONGES_PAYES', 'Congés Payés', 'Congés annuels payés', 30),
('RTT', 'Réduction Temps de Travail', 'Jours de RTT', 10),
('CONGE_PARENTAL', 'Congé Parental', 'Congé parental', 180),
('CONGE_MALADIE', 'Congé Maladie', 'Congé pour maladie', 0),
('CONGE_SANS_SOLDE', 'Congé Sans Solde', 'Congé non rémunéré', 0);

-- Insérer les statuts de congés
INSERT IGNORE INTO statut_conge (code, nom) VALUES
('EN_ATTENTE', 'En Attente'),
('APPROUVE', 'Approuvé'),
('REJETE', 'Rejeté'),
('ANNULE', 'Annulé');

-- Créer un utilisateur administrateur par défaut (mot de passe: admin123)
-- Note: Assurez-vous que le mot de passe est hashé avant l'insertion!
INSERT IGNORE INTO user_entity (id, username, email, password, full_name, role_id, active) VALUES
(1, 'admin', 'admin@company.com', '$2a$10$sKLR2e26MSxaL7c3gvH7ke5VWnGHtZ5X/qV9X8y7r/7VUJx.qrv9u', 'Administrateur', 3, TRUE);

-- ============================================
-- DB HISTORY / OUTBOX (conges_db)
-- ============================================

CREATE TABLE IF NOT EXISTS history_outbox (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    payload JSON NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    attempts INT NOT NULL DEFAULT 0,
    last_error TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_status_created (status, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE utf8mb4_unicode_ci;

-- ============================================
-- DB DOLIBARR (minimal tables for allocations)
-- ============================================

USE dolibarr;

CREATE TABLE IF NOT EXISTS leave_type (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    nom VARCHAR(255) NOT NULL,
    description TEXT,
    jours_autorises INT DEFAULT 30,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS employee_leave_allocation (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    leave_type_id BIGINT NOT NULL,
    annee_fiscale INT NOT NULL,
    solde_initial INT DEFAULT 30,
    solde_consomme INT DEFAULT 0,
    solde_restant INT DEFAULT 30,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_allocation (user_id, leave_type_id, annee_fiscale),
    INDEX idx_user_type_year (user_id, leave_type_id, annee_fiscale)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE utf8mb4_unicode_ci;

INSERT IGNORE INTO leave_type (code, nom, description, jours_autorises) VALUES
('CONGES_PAYES', 'Congés Payés', 'Congés annuels payés', 30),
('RTT', 'Réduction Temps de Travail', 'Jours de RTT', 10),
('CONGE_PARENTAL', 'Congé Parental', 'Congé parental', 180),
('CONGE_MALADIE', 'Congé Maladie', 'Congé pour maladie', 0),
('CONGE_SANS_SOLDE', 'Congé Sans Solde', 'Congé non rémunéré', 0);
