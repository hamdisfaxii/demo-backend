-- Script SQL pour créer la table history
-- Cette table est aussi créée par Hibernate avec @Entity

CREATE TABLE IF NOT EXISTS history (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    demande_id BIGINT,
    action_type VARCHAR(50) NOT NULL,
    description VARCHAR(500),
    details LONGTEXT,
    pays VARCHAR(50),
    statut VARCHAR(50),
    action_date DATETIME NOT NULL,
    ip_address VARCHAR(45),
    user_agent VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    -- Ne pas mettre ON DELETE CASCADE sur user_id : en local, recréer / nettoyer les users efface tout l’historique.
    CONSTRAINT fk_history_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE RESTRICT,
    CONSTRAINT fk_history_demande FOREIGN KEY (demande_id) REFERENCES demandes_conge(id) ON DELETE SET NULL,
    
    INDEX idx_user (user_id),
    INDEX idx_demande (demande_id),
    INDEX idx_action_type (action_type),
    INDEX idx_action_date (action_date)
) 
ENGINE=InnoDB 
DEFAULT CHARSET=utf8mb4 
COLLATE=utf8mb4_unicode_ci;

-- Inserts de test (optionnel)
-- INSERT INTO history (user_id, action_type, description, action_date) VALUES 
-- (1, 'LOGIN', 'Connexion utilisateur', NOW());

-- Si la table existe déjà avec CASCADE sur user_id, inspecter le nom exact de la contrainte :
--   SHOW CREATE TABLE history;
-- Puis remplacer fk_history_user par le nom réel, par ex. :
-- ALTER TABLE history DROP FOREIGN KEY fk_history_user;
-- ALTER TABLE history ADD CONSTRAINT fk_history_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE RESTRICT;
