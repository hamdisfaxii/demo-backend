package com.example.conges.service;

import com.example.conges.entity.CountryLeavePolicy;
import com.example.conges.entity.TypeConge;
import com.example.conges.repository.CountryLeavePolicyRepository;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CountryPolicyService {

    private static final Map<TypeConge, Integer> DEFAULT_POLICY = new EnumMap<>(TypeConge.class);

    static {
        DEFAULT_POLICY.put(TypeConge.PAYE, 30);
        DEFAULT_POLICY.put(TypeConge.COURTE_DUREE, 5);
        DEFAULT_POLICY.put(TypeConge.MALADIE, 0);
        DEFAULT_POLICY.put(TypeConge.SANS_SOLDE, 0);
    }

    private final CountryLeavePolicyRepository countryLeavePolicyRepository;

    public int getAnnualQuota(String countryCode, TypeConge typeConge) {
        String normalized = normalizeCountry(countryCode);
        return countryLeavePolicyRepository.findFirstByCountryCodeAndTypeConge(normalized, typeConge)
                .map(CountryLeavePolicy::getAnnualQuota)
                .orElseGet(() -> DEFAULT_POLICY.getOrDefault(typeConge, 0));
    }

    @Transactional(readOnly = true)
    public List<CountryLeavePolicy> getAllPolicies() {
        return countryLeavePolicyRepository.findAll();
    }

    @Transactional
    public CountryLeavePolicy upsertPolicy(String countryCode, TypeConge typeConge, Integer annualQuota) {
        String normalized = normalizeCountry(countryCode);
        CountryLeavePolicy policy = countryLeavePolicyRepository
                .findFirstByCountryCodeAndTypeConge(normalized, typeConge)
                .orElseGet(() -> CountryLeavePolicy.builder()
                        .countryCode(normalized)
                        .typeConge(typeConge)
                        .build());
        policy.setAnnualQuota(annualQuota);
        return countryLeavePolicyRepository.save(policy);
    }

    private String normalizeCountry(String countryCode) {
        if (countryCode == null || countryCode.isBlank()) {
            return "FR";
        }
        return countryCode.trim().toUpperCase(Locale.ROOT);
    }
}
