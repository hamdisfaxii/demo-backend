package com.example.conges.controller;

import com.example.conges.dto.config.CountryPolicyConfigRequest;
import com.example.conges.dto.config.LeaveTypeConfigRequest;
import com.example.conges.entity.CountryLeavePolicy;
import com.example.conges.entity.LeaveType;
import com.example.conges.service.CountryPolicyService;
import com.example.conges.service.HrConfigService;
import java.util.List;
import java.util.Map;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/hr-config")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('RH','ADMIN')")
public class HrConfigController {

    private final CountryPolicyService countryPolicyService;
    private final HrConfigService hrConfigService;

    @GetMapping("/country-policies")
    public ResponseEntity<List<CountryLeavePolicy>> getCountryPolicies() {
        return ResponseEntity.ok(hrConfigService.getCountryPolicies());
    }

    @PostMapping("/country-policies")
    public ResponseEntity<CountryLeavePolicy> upsertCountryPolicy(
            @Valid @RequestBody CountryPolicyConfigRequest request
    ) {
        return ResponseEntity.ok(countryPolicyService.upsertPolicy(
                request.getCountryCode(),
                request.getTypeConge(),
                request.getAnnualQuota()
        ));
    }

    @GetMapping("/leave-types")
    public ResponseEntity<List<LeaveType>> getLeaveTypes() {
        return ResponseEntity.ok(hrConfigService.getLeaveTypes());
    }

    @PutMapping("/leave-types/{id}")
    public ResponseEntity<LeaveType> updateLeaveType(
            @PathVariable Long id,
            @Valid @RequestBody LeaveTypeConfigRequest request
    ) {
        return ResponseEntity.ok(hrConfigService.updateLeaveType(id, request));
    }

    @GetMapping("/integration-settings")
    public ResponseEntity<Map<String, String>> getIntegrationSettings() {
        return ResponseEntity.ok(hrConfigService.getIntegrationSettings());
    }
}
