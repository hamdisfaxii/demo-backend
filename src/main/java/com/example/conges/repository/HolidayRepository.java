package com.example.conges.repository;

import com.example.conges.entity.Holiday;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface HolidayRepository extends JpaRepository<Holiday, Long> {

    Optional<Holiday> findByDolibarrHolidayId(Long dolibarrHolidayId);

    List<Holiday> findByActiveTrue();

    @Query("SELECT h FROM Holiday h WHERE h.active = true AND h.dateJour BETWEEN :debut AND :fin ORDER BY h.dateJour ASC")
    List<Holiday> findHolidaysBetween(LocalDate debut, LocalDate fin);

    @Query("SELECT h FROM Holiday h WHERE h.active = true AND YEAR(h.dateJour) = :year")
    List<Holiday> findHolidaysByYear(Integer year);

    @Query("SELECT h FROM Holiday h WHERE h.countryCode = :countryCode AND h.active = true AND h.dateJour BETWEEN :startDate AND :endDate ORDER BY h.dateJour ASC")
    List<Holiday> findByCountryCodeAndDateRange(
            @Param("countryCode") String countryCode,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("""
            SELECT h
            FROM Holiday h
            WHERE h.active = true
              AND h.dateJour BETWEEN :startDate AND :endDate
              AND (:countryCode IS NULL OR h.countryCode = :countryCode)
            ORDER BY h.dateJour ASC
            """)
    List<Holiday> findByDateRangeWithOptionalCountry(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("countryCode") String countryCode
    );
}
