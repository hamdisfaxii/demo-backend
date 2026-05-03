package com.example.conges.repository;

import com.example.conges.entity.WorkScheduleDay;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WorkScheduleDayRepository extends JpaRepository<WorkScheduleDay, Long> {

    List<WorkScheduleDay> findByCountryCodeAndScheduleTypeOrderByDayOfWeekAsc(
            String countryCode,
            String scheduleType
    );
}
