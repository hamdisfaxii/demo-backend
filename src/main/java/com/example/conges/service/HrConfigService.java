package com.example.conges.service;

import com.example.conges.dto.config.LeaveTypeConfigRequest;
import com.example.conges.entity.CountryLeavePolicy;
import com.example.conges.entity.ExceptionalLeaveConfig;
import com.example.conges.entity.LeaveType;
import com.example.conges.repository.ExceptionalLeaveConfigRepository;
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
    private final ExceptionalLeaveConfigRepository exceptionalLeaveConfigRepository;
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

    @Transactional(readOnly = true)
    public List<ExceptionalLeaveConfig> getExceptionalLeavesByCountry(String countryCode) {
        String normalized = countryCode == null ? "TN" : countryCode.trim().toUpperCase();
        return exceptionalLeaveConfigRepository.findByCountryCodeOrderByLabelAsc(normalized);
    }

    @Transactional
    public ExceptionalLeaveConfig createExceptionalLeave(ExceptionalLeaveConfig payload) {
        payload.setId(null);
        payload.setCountryCode(payload.getCountryCode() == null ? "TN" : payload.getCountryCode().trim().toUpperCase());
        payload.setLabel(payload.getLabel() == null ? "" : payload.getLabel().trim());
        if (payload.getEnabled() == null) {
            payload.setEnabled(Boolean.TRUE);
        }
        if (payload.getDaysPerYear() == null) {
            payload.setDaysPerYear(0);
        }
        return exceptionalLeaveConfigRepository.save(payload);
    }

    @Transactional
    public ExceptionalLeaveConfig updateExceptionalLeave(Long id, ExceptionalLeaveConfig payload) {
        ExceptionalLeaveConfig existing = exceptionalLeaveConfigRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Congé exceptionnel introuvable"));
        if (payload.getCountryCode() != null) {
            existing.setCountryCode(payload.getCountryCode().trim().toUpperCase());
        }
        if (payload.getLabel() != null) {
            existing.setLabel(payload.getLabel().trim());
        }
        if (payload.getDaysPerYear() != null) {
            existing.setDaysPerYear(payload.getDaysPerYear());
        }
        if (payload.getEnabled() != null) {
            existing.setEnabled(payload.getEnabled());
        }
        return exceptionalLeaveConfigRepository.save(existing);
    }
}
