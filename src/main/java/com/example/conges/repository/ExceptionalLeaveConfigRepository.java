package com.example.conges.repository;

import com.example.conges.entity.ExceptionalLeaveConfig;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ExceptionalLeaveConfigRepository extends JpaRepository<ExceptionalLeaveConfig, Long> {

    List<ExceptionalLeaveConfig> findByCountryCodeOrderByLabelAsc(String countryCode);
}
