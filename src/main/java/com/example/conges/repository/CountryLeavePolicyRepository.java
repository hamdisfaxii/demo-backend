package com.example.conges.repository;

import com.example.conges.entity.CountryLeavePolicy;
import com.example.conges.entity.TypeConge;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CountryLeavePolicyRepository extends JpaRepository<CountryLeavePolicy, Long> {
    Optional<CountryLeavePolicy> findFirstByCountryCodeAndTypeConge(String countryCode, TypeConge typeConge);
}
