package com.example.conges.controller;

import com.example.conges.entity.ExceptionalLeaveConfig;
import com.example.conges.entity.StatutConge;
import com.example.conges.entity.UserEntity;
import com.example.conges.repository.DemandeCongeRepository;
import com.example.conges.repository.ExceptionalLeaveConfigRepository;
import com.example.conges.service.CountryPolicyService;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import javax.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/exceptional-leaves")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class ExceptionalLeaveEmployeeController {

    private final CountryPolicyService countryPolicyService;
    private final ExceptionalLeaveConfigRepository exceptionalLeaveConfigRepository;
    private final DemandeCongeRepository demandeCongeRepository;

    @GetMapping("/available")
    public ResponseEntity<List<Map<String, Object>>> available(@AuthenticationPrincipal @NotNull UserEntity actor) {
        String country = countryPolicyService.normalizeBusinessCountry(
                actor.getCountryCode() == null ? actor.getPays() : actor.getCountryCode());
        List<ExceptionalLeaveConfig> configs = exceptionalLeaveConfigRepository
                .findByCountryCodeOrderByLabelAsc(country)
                .stream()
                .filter(c -> Boolean.TRUE.equals(c.getEnabled()))
                .toList();
        int year = LocalDate.now().getYear();
        List<Map<String, Object>> out = configs.stream().map(cfg -> {
            double usedOrPending = demandeCongeRepository.sumExceptionalDaysForUserAndConfig(
                    actor.getId(),
                    cfg.getId(),
                    year,
                    EnumSet.of(StatutConge.EN_ATTENTE, StatutConge.ACCEPTE));
            double quota = cfg.getDaysPerYear() == null ? 0d : Math.max(0d, cfg.getDaysPerYear());
            double remaining = Math.max(0d, quota - usedOrPending);
            return Map.<String, Object>of(
                    "id", cfg.getId(),
                    "label", cfg.getLabel(),
                    "daysPerYear", cfg.getDaysPerYear(),
                    "remainingDays", remaining
            );
        }).toList();
        return ResponseEntity.ok(out);
    }
}

