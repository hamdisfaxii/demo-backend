package com.example.conges.repository;

import com.example.conges.entity.DemandeAttachment;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DemandeAttachmentRepository extends JpaRepository<DemandeAttachment, Long> {
    List<DemandeAttachment> findByDemande_IdOrderByUploadedAtDesc(Long demandeId);
}

