-- Module RTT France : contrat (users) + paramètres + solde annuel local

ALTER TABLE users
    ADD COLUMN weekly_hours DECIMAL(5, 2) NULL,
    ADD COLUMN annual_work_days INT NULL,
    ADD COLUMN contract_type VARCHAR(48) NULL,
    ADD COLUMN contract_active TINYINT(1) NOT NULL DEFAULT 1,
    ADD COLUMN country_code VARCHAR(8) NULL;

CREATE TABLE IF NOT EXISTS france_rtt_settings (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    accrual_mode VARCHAR(32) NOT NULL DEFAULT 'CONTRACT_HOURS',
    admin_override_days INT NULL,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

INSERT INTO france_rtt_settings (id, accrual_mode, admin_override_days)
VALUES (1, 'CONTRACT_HOURS', NULL)
ON DUPLICATE KEY UPDATE id = id;

CREATE TABLE IF NOT EXISTS employee_france_rtt_balance (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    calendar_year INT NOT NULL,
    rtt_total DECIMAL(10, 2) NOT NULL DEFAULT 0,
    rtt_used DECIMAL(10, 2) NOT NULL DEFAULT 0,
    rtt_remaining DECIMAL(10, 2) NOT NULL DEFAULT 0,
    last_rtt_update DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_fr_rtt_bal_user FOREIGN KEY (user_id) REFERENCES users(id),
    UNIQUE KEY uk_user_year (user_id, calendar_year)
);
