package com.example.conges.service;

import com.example.conges.dto.config.LeaveTypeConfigRequest;
import com.example.conges.entity.CountryLeavePolicy;
import com.example.conges.entity.LeaveType;
import com.example.conges.repository.LeaveTypeRepository;
import java.util.List;
import java.util.Map;
import javax.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class HrConfigService {

    private final CountryPolicyService countryPolicyService;
    private final LeaveTypeRepository leaveTypeRepository;
    private final DolibarrService dolibarrService;

    @Transactional(readOnly = true)
    public List<CountryLeavePolicy> getCountryPolicies() {
        return countryPolicyService.getAllPolicies();
    }

    @Transactional(readOnly = true)
    public List<LeaveType> getLeaveTypes() {
        return leaveTypeRepository.findAll();
    }

    @Transactional
    public LeaveType updateLeaveType(Long id, LeaveTypeConfigRequest request) {
        LeaveType leaveType = leaveTypeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Type de congé introuvable"));
        if (request.getLibelle() != null) {
            leaveType.setLibelle(request.getLibelle());
        }
        if (request.getDescription() != null) {
            leaveType.setDescription(request.getDescription());
        }
        if (request.getCouleur() != null) {
            leaveType.setCouleur(request.getCouleur());
        }
        if (request.getActive() != null) {
            leaveType.setActive(request.getActive());
        }
        if (request.getRequiresApproval() != null) {
            leaveType.setRequiresApproval(request.getRequiresApproval());
        }
        if (request.getDelai() != null) {
            leaveType.setDelai(request.getDelai());
        }
        return leaveTypeRepository.save(leaveType);
    }

    @Transactional(readOnly = true)
    public Map<String, String> getIntegrationSettings() {
        return Map.of("dolibarrStatus", dolibarrService.getDolibarrStatus());
    }
}
