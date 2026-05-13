package com.example.conges.service;

import com.example.conges.config.FranceRttProperties;
import com.example.conges.dto.config.EmployeeContractRhPatchRequest;
import com.example.conges.dto.config.FranceRttSettingsDto;
import com.example.conges.dto.config.LeaveTypeConfigRequest;
import com.example.conges.entity.CountryLeavePolicy;
import com.example.conges.entity.ExceptionalLeaveConfig;
import com.example.conges.entity.FranceRttSettings;
import com.example.conges.entity.LeaveType;
import com.example.conges.entity.UserEntity;
import com.example.conges.repository.ExceptionalLeaveConfigRepository;
import com.example.conges.repository.LeaveTypeRepository;
import com.example.conges.repository.UserRepository;
import java.time.LocalDate;
import java.time.Year;
import java.util.HashMap;
import java.util.LinkedHashMap;
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
    private final FranceRttLedgerService franceRttLedgerService;
    private final FranceRttProperties franceRttProperties;
    private final UserRepository userRepository;

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
        List<ExceptionalLeaveConfig> existing =
                exceptionalLeaveConfigRepository.findByCountryCodeOrderByLabelAsc(normalized);
        if (existing != null && !existing.isEmpty()) {
            return existing;
        }
        // Auto-seed minimal defaults per country so RH sees a usable starting point.
        seedDefaultExceptionalLeavesIfMissing(normalized);
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
        if (payload.getCountryCode() != null && !payload.getCountryCode().isBlank()) {
            String requestedCountry = payload.getCountryCode().trim().toUpperCase();
            String currentCountry = String.valueOf(existing.getCountryCode()).trim().toUpperCase();
            if (!requestedCountry.equals(currentCountry)) {
                throw new IllegalArgumentException("Accès refusé: ce congé appartient à un autre pays.");
            }
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

    @Transactional
    public void deleteExceptionalLeave(Long id) {
        if (id == null) return;
        exceptionalLeaveConfigRepository.deleteById(id);
    }

    @Transactional
    protected void seedDefaultExceptionalLeavesIfMissing(String countryCode) {
        String cc = countryCode == null ? "TN" : countryCode.trim().toUpperCase();
        if (!exceptionalLeaveConfigRepository.findByCountryCodeOrderByLabelAsc(cc).isEmpty()) {
            return;
        }
        // Default suggestions (RH can edit durations anytime).
        Map<String, Integer> defaults = defaultExceptionalLeavesForCountry(cc);
        for (Map.Entry<String, Integer> e : defaults.entrySet()) {
            exceptionalLeaveConfigRepository.save(ExceptionalLeaveConfig.builder()
                    .countryCode(cc)
                    .label(e.getKey())
                    .daysPerYear(Math.max(0, e.getValue() == null ? 0 : e.getValue()))
                    .enabled(Boolean.TRUE)
                    .build());
        }
    }

    private Map<String, Integer> defaultExceptionalLeavesForCountry(String cc) {
        Map<String, Integer> out = new LinkedHashMap<>();
        switch (cc) {
            case "FR" -> {
                out.put("Congé mariage", 4);
                out.put("Congé naissance / accouchement", 3);
                out.put("Congé décès", 3);
                out.put("Congé exceptionnel familial", 2);
            }
            case "MA" -> {
                out.put("Congé mariage", 4);
                out.put("Congé naissance / accouchement", 3);
                out.put("Congé décès", 2);
                out.put("Congé exceptionnel familial", 2);
            }
            default -> {
                out.put("Congé mariage", 3);
                out.put("Congé naissance / accouchement", 3);
                out.put("Congé décès", 2);
                out.put("Congé exceptionnel familial", 2);
            }
        }
        return out;
    }

    @Transactional(readOnly = true)
    public FranceRttSettingsDto getFranceRttSettings() {
        FranceRttSettings s = franceRttLedgerService.resolvedSettingsSnapshot();
        FranceRttSettingsDto dto = new FranceRttSettingsDto();
        dto.setAccrualMode(s.getAccrualMode());
        dto.setAdminOverrideDays(s.getAdminOverrideDays());
        dto.setUpdatedAt(s.getUpdatedAt());
        return dto;
    }

    public Map<String, Object> franceRttFeatureFlags() {
        Map<String, Object> meta = new HashMap<>();
        meta.put("localLedgerEnabled", franceRttProperties.isLocalLedgerEnabled());
        meta.put("skipDolibarrRttConsume", franceRttProperties.isSkipDolibarrRttConsume());
        return meta;
    }

    @Transactional
    public FranceRttSettingsDto upsertFranceRttSettings(FranceRttSettingsDto body) {
        franceRttLedgerService.upsertGlobalSettings(
                body.getAccrualMode(), body.getAdminOverrideDays());
        return getFranceRttSettings();
    }

    /**
     * Ajustements contrat (RTT FR notamment). Resynchronise le cache SQL lorsque pertinent.
     */
    @Transactional
    public UserEntity patchEmployeeContract(Long employeeId, EmployeeContractRhPatchRequest request) {
        UserEntity user = userRepository.findById(employeeId)
                .orElseThrow(() -> new EntityNotFoundException("Employé introuvable"));
        if (request.getWeeklyHours() != null) {
            user.setWeeklyHours(request.getWeeklyHours());
        }
        if (request.getAnnualWorkDays() != null) {
            user.setAnnualWorkDays(request.getAnnualWorkDays());
        }
        if (request.getContractType() != null) {
            user.setContractType(request.getContractType().trim());
        }
        if (request.getContractActive() != null) {
            user.setContractActive(Boolean.TRUE.equals(request.getContractActive()));
        }
        if (request.getCountryCode() != null && !request.getCountryCode().isBlank()) {
            user.setCountryCode(request.getCountryCode().trim().toUpperCase());
        }
        userRepository.save(user);
        try {
            if (franceRttLedgerService.usesPersistedFranceRttLedger(user)) {
                franceRttLedgerService.refreshPersistedTotals(
                        employeeId, Year.now().getValue(), LocalDate.now());
            }
        } catch (RuntimeException ignored) {
            // Profils hors périmètre (>35 h) ou seed incomplet ; projection GET recalculera.
        }
        return user;
    }
}
