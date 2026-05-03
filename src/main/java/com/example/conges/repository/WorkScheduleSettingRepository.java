package com.example.conges.repository;

import com.example.conges.entity.WorkScheduleSetting;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WorkScheduleSettingRepository extends JpaRepository<WorkScheduleSetting, Long> {

    Optional<WorkScheduleSetting> findByCountryCode(String countryCode);
}
