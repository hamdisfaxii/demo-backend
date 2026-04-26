package com.example.conges.repository;

import com.example.conges.entity.Role;
import com.example.conges.entity.WorkflowStep;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkflowStepRepository extends JpaRepository<WorkflowStep, Long> {
    List<WorkflowStep> findByWorkflowDefinitionIdOrderByStepOrderAsc(Long workflowDefinitionId);
    List<WorkflowStep> findByApproverRole(Role approverRole);
}
