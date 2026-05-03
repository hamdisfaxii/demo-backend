package com.example.conges.repository;

import com.example.conges.entity.EmployeeFranceRttBalance;
import com.example.conges.entity.UserEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EmployeeFranceRttBalanceRepository extends JpaRepository<EmployeeFranceRttBalance, Long> {

    Optional<EmployeeFranceRttBalance> findByUserAndCalendarYear(UserEntity user, int calendarYear);

    @Query("""
            SELECT b FROM EmployeeFranceRttBalance b
            JOIN FETCH b.user u
            WHERE u.id = :userId AND b.calendarYear = :year""")
    Optional<EmployeeFranceRttBalance> findFetchedByUserIdAndYear(@Param("userId") Long userId, @Param("year") int year);

    @Query("SELECT b FROM EmployeeFranceRttBalance b JOIN FETCH b.user u WHERE b.calendarYear = :year")
    List<EmployeeFranceRttBalance> findAllFetchedByYear(@Param("year") int year);
}
