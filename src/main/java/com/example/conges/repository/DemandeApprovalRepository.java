package com.example.conges.repository;

import com.example.conges.entity.DemandeApproval;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DemandeApprovalRepository extends JpaRepository<DemandeApproval, Long> {
    List<DemandeApproval> findByDemandeIdOrderByStepOrderAscDecisionDateAsc(Long demandeId);
}
