package com.example.conges.repository;

import com.example.conges.entity.WorkflowDefinition;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkflowDefinitionRepository extends JpaRepository<WorkflowDefinition, Long> {
    Optional<WorkflowDefinition> findFirstByCountryCodeAndActiveTrue(String countryCode);
    Optional<WorkflowDefinition> findFirstByCodeAndActiveTrue(String code);
}
