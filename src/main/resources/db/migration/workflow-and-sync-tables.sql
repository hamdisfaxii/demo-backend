-- Workflow dynamique multi-niveaux
CREATE TABLE IF NOT EXISTS workflow_definitions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    code VARCHAR(100) NOT NULL UNIQUE,
    country_code VARCHAR(10) NOT NULL,
    active BIT NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL
);

CREATE TABLE IF NOT EXISTS workflow_steps (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    workflow_definition_id BIGINT NOT NULL,
    step_order INT NOT NULL,
    step_type VARCHAR(50) NOT NULL,
    approver_role VARCHAR(20) NOT NULL,
    required BIT NOT NULL DEFAULT 1,
    CONSTRAINT fk_workflow_steps_definition FOREIGN KEY (workflow_definition_id) REFERENCES workflow_definitions(id)
);

CREATE TABLE IF NOT EXISTS demande_approvals (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    demande_id BIGINT NOT NULL,
    step_order INT NOT NULL,
    step_type VARCHAR(50) NOT NULL,
    actor_user_id BIGINT NOT NULL,
    decision VARCHAR(20) NOT NULL,
    comment VARCHAR(1000),
    decision_date DATETIME NOT NULL,
    CONSTRAINT fk_demande_approvals_demande FOREIGN KEY (demande_id) REFERENCES demandes_conge(id),
    CONSTRAINT fk_demande_approvals_actor FOREIGN KEY (actor_user_id) REFERENCES users(id)
);

-- Politiques multi-pays extensibles
CREATE TABLE IF NOT EXISTS country_leave_policies (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    country_code VARCHAR(10) NOT NULL,
    type_conge VARCHAR(32) NOT NULL,
    annual_quota INT NOT NULL
);

-- Logs techniques d'intégration Dolibarr
CREATE TABLE IF NOT EXISTS dolibarr_sync_logs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    entity_type VARCHAR(50) NOT NULL,
    operation VARCHAR(50),
    local_entity_id BIGINT,
    remote_entity_id BIGINT,
    direction VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    message VARCHAR(1000),
    payload LONGTEXT,
    created_at DATETIME NOT NULL
);
